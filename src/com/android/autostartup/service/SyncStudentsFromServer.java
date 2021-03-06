package com.android.autostartup.service;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.android.autostartup.controller.server.Server;
import com.android.autostartup.dao.StudentDao;
import com.android.autostartup.model.Student;

public class SyncStudentsFromServer {

    private static final String TAG = SyncStudentsFromServer.class.getSimpleName();

    private StudentDao studentDao;

    private Student[] studentsFromServer;
    private List<Long> newIds = new ArrayList<Long>();
    private List<Long> updatedIds = new ArrayList<Long>();

    public SyncStudentsFromServer(Context context) {
        studentDao = new StudentDao(context);
    }

    public void updateStudentDataFromServer() {

        Server.requestAllStudentIds(new Server.GetStudentsCallback() {
            @Override
            public void onSuccess(Student[] students) {
                studentsFromServer = students;
                syncData();
            }
        }, new Server.ErrorCallback() {

            @Override
            public void onFail(String reason) {
                Log.e(TAG, "get student ids list failed");
            }
        });

    }

    private void syncData() {
        List<Student> studentFromLocal = studentDao.getAll();
        if (null == studentFromLocal || studentFromLocal.isEmpty()) {
            Log.i(TAG, "save all ids");
            saveAllStudents();
        } else {
            seperateData(studentFromLocal);

            saveStudents();
            updateStudents();

        }
    }

    private void seperateData(List<Student> students) {
        // TODO Refactor
        List<Student> localStudents = students;
        List<Student> serverStudents = new ArrayList<Student>();
        for (Student student : studentsFromServer) {
            serverStudents.add(student);
        }
        Log.i(TAG, "ServerStudentList.size=" + serverStudents.size());

        for (int i = serverStudents.size() - 1; i > -1; i--) {
            Student serverObj = serverStudents.get(i);
            long serverId = serverObj.id;
            for (int j = localStudents.size() - 1; j > -1; j--) {
                Student localObj = localStudents.get(j);

                if (serverId == localObj.id) {
                    if (serverObj.updatedAt != localObj.updatedAt) {
                        updatedIds.add(serverId);
                    }
                    localStudents.remove(j);
                    serverStudents.remove(i);
                    break;
                }
            }
        }

        Log.i(TAG, "serverstudentlist.size=" + serverStudents.size());
        for (Student student : serverStudents) {
            newIds.add(student.id);
        }
    }

    private void updateStudents() {
        if (!updatedIds.isEmpty()) {
            String ids = TextUtils.join(",", updatedIds);
            Log.i(TAG, "update updated ids:" + ids);
            updateStudents(ids);
            updatedIds.clear();
        }
    }

    private void saveStudents() {
        if (!newIds.isEmpty()) {
            String ids = TextUtils.join(",", newIds);
            Log.i(TAG, "save new ids" + ids);
            saveNewStudents(ids);
            newIds.clear();
        }
    }

    private void saveAllStudents() {
        Server.requestAllStudent(new Server.GetStudentsCallback() {
            @Override
            public void onSuccess(Student[] students) {
                for (Student student : students) {
                    Log.i(TAG, student.toString());
                }
                studentDao.loadAndSavePics(students);
                studentDao.save(students);
            }
        }, new Server.ErrorCallback() {

            @Override
            public void onFail(String reason) {
                Log.e(TAG, "get student list failed");
            }
        });
    }

    private void saveNewStudents(String ids) {
        Server.requestStudentsByIds(ids, new Server.GetStudentsCallback() {

            @Override
            public void onSuccess(Student[] students) {
                studentDao.loadAndSavePics(students);
                studentDao.save(students);

            }
        }, new Server.ErrorCallback() {

            @Override
            public void onFail(String reason) {
                Log.e(TAG, "get student list failed");
            }
        });

    }

    private void updateStudents(String ids) {
        Server.requestStudentsByIds(ids, new Server.GetStudentsCallback() {

            @Override
            public void onSuccess(Student[] students) {
                studentDao.updateById(students);

            }
        }, new Server.ErrorCallback() {

            @Override
            public void onFail(String reason) {
                Log.e(TAG, "get student list failed");
            }
        });
    }
}
