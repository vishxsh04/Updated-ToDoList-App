package com.example.todolist.Model;

public class ToDoModel {
    private int id,status;
    private String task,category,priority,duedate;

    public int getId() {
        return id;
    }

    public void setId(int id) {

        this.id = id;
    }

    public int getStatus() {

        return status;
    }

    public void setStatus(int status) {

        this.status = status;
    }

    public String getTask() {

        return task;
    }

    public void setTask(String task) {

        this.task = task;
    }

    public String getCategory(){
        return category;
    }
    public void setCategory(String category) {
        this.category = category;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getDueDate() {
        return duedate;
    }

    public void setDueDate(String duedate) {
        this.duedate = duedate;
    }

}
