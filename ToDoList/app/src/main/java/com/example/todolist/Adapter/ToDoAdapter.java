package com.example.todolist.Adapter;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.todolist.AddNewTask;
import com.example.todolist.MainActivity;
import com.example.todolist.Model.ToDoModel;
import com.example.todolist.R;
import com.example.todolist.Utils.DatabaseHandler;

import java.util.List;

public class ToDoAdapter extends RecyclerView.Adapter<ToDoAdapter.ViewHolder> {

    private List<ToDoModel> todoList;
    private MainActivity activity;
    private DatabaseHandler db;

    // Constructor
    public ToDoAdapter(DatabaseHandler db, MainActivity activity) {
        this.db = db;
        this.activity = activity;
    }

    // ViewHolder class with all views
    public class ViewHolder extends RecyclerView.ViewHolder {
        CheckBox task;
        TextView categoryText, priorityText, dueDateText;

        public ViewHolder(View itemView) {
            super(itemView);
            task = itemView.findViewById(R.id.todoCheckBox);
            categoryText = itemView.findViewById(R.id.categoryText);
            priorityText = itemView.findViewById(R.id.priorityText);
            dueDateText = itemView.findViewById(R.id.dueDateText);
        }
    }

    // Inflate the item layout and create the holder
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.task_layout, parent, false);
        return new ViewHolder(itemView);
    }

    // Bind data to the views
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ToDoModel item = todoList.get(position);

        // Remove old listener before changing checked state to avoid unwanted triggers
        holder.task.setOnCheckedChangeListener(null);

        // Set task title and checked status
        holder.task.setText(item.getTask());
        holder.task.setChecked(toBoolean(item.getStatus()));

        // Set category, priority, due date
        holder.categoryText.setText("Category: " + item.getCategory());
        holder.priorityText.setText("Priority: " + item.getPriority());
        holder.dueDateText.setText("Due: " + item.getDueDate());

        // Checkbox listener to update status on manual checkbox click
        holder.task.setOnCheckedChangeListener((buttonView, isChecked) -> {
            int status = isChecked ? 1 : 0;
            item.setStatus(status);
            db.updateStatus(item.getId(), status);
        });

        // ItemView click toggles checkbox and updates status
        holder.itemView.setOnClickListener(v -> {
            boolean newStatus = !holder.task.isChecked();  // Toggle current checkbox state
            holder.task.setChecked(newStatus);             // Update checkbox UI
            item.setStatus(newStatus ? 1 : 0);
            db.updateStatus(item.getId(), item.getStatus());
        });

        // **Long press listener for edit/delete options**
        holder.itemView.setOnLongClickListener(v -> {
            CharSequence[] options = {"Edit", "Delete"};
            new androidx.appcompat.app.AlertDialog.Builder(activity)
                    .setTitle("Choose an option")
                    .setItems(options, (dialog, which) -> {
                        if (which == 0) {
                            editItem(position);
                        } else if (which == 1) {
                            // Show confirmation dialog before deleting
                            new androidx.appcompat.app.AlertDialog.Builder(activity)
                                    .setTitle("Delete Task")
                                    .setMessage("Are you sure you want to delete this task?")
                                    .setPositiveButton("CONFIRM", (dialog1, which1) -> {
                                        deleteItem(position);
                                    })
                                    .setNegativeButton("CANCEL", null)
                                    .show();
                        }
                    })
                    .show();
            return true;  // Indicate that the long press is handled
        });

    }

    @Override
    public int getItemCount() {
        return todoList != null ? todoList.size() : 0;
    }

    private boolean toBoolean(int n) {
        return n != 0;
    }

    // Update adapter list and refresh UI
    public void setTasks(List<ToDoModel> todoList) {
        this.todoList = todoList;
        notifyDataSetChanged();
    }

    public Context getContext() {
        return activity;
    }

    // Delete task at position
    public void deleteItem(int position) {
        ToDoModel item = todoList.get(position);
        db.deleteTask(item.getId());
        todoList.remove(position);
        notifyItemRemoved(position);
    }

    // Edit task at position
    public void editItem(int position) {
        ToDoModel item = todoList.get(position);
        activity.showEditTaskDialog(item);  // Call MainActivity method with the task
    }


}
