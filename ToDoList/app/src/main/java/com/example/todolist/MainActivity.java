package com.example.todolist;

import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.todolist.Adapter.ToDoAdapter;
import com.example.todolist.Model.ToDoModel;
import com.example.todolist.Utils.DatabaseHandler;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

import android.widget.EditText;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.Toast;


public class MainActivity extends AppCompatActivity implements DialogCloseListener {

    private RecyclerView tasksRecyclerView;
    private ToDoAdapter tasksAdapter;
    private List<ToDoModel> taskList;
    private FloatingActionButton fab;
    public DatabaseHandler db;
    private String currentCategoryFilter = null;
    private String currentPriorityFilter = null;
    private TextView activeFiltersText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        createNotificationChannel();


        // 🔐 Request POST_NOTIFICATIONS permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 100);
            }
        }

        // Hide ActionBar if present
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        db = new DatabaseHandler(this);
        db.openDatabase();

        taskList = new ArrayList<>();

        activeFiltersText = findViewById(R.id.activeFiltersText);
        updateActiveFiltersText();

        Button filterBtn = findViewById(R.id.filterBtn);
        filterBtn.setOnClickListener(v -> showFilterDialog());

        tasksRecyclerView = findViewById(R.id.tasksRecyclerView);
        tasksRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        tasksAdapter = new ToDoAdapter(db, this);
        tasksRecyclerView.setAdapter(tasksAdapter);

        fab = findViewById(R.id.default_activity_button);
        fab.setOnClickListener(v -> showAddTaskDialog());
        fab.setOnClickListener(v -> showTaskDialog(null));

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new RecyclerItemTouchHelper(tasksAdapter));
        itemTouchHelper.attachToRecyclerView(tasksRecyclerView);

        taskList = db.getAllTasks();
        Collections.reverse(taskList);
        tasksAdapter.setTasks(taskList);
    }

    private void showTaskDialog(@Nullable ToDoModel taskToEdit) {
        View view = getLayoutInflater().inflate(R.layout.new_task, null);

        EditText taskEditText = view.findViewById(R.id.newTaskText);
        Spinner categorySpinner = view.findViewById(R.id.category_spinner);
        Spinner prioritySpinner = view.findViewById(R.id.priority_spinner);
        TextView dueDateText = view.findViewById(R.id.due_date_text);
        Button selectDateButton = view.findViewById(R.id.select_date_button);

        // Populate spinners
        String[] categories = {"Work", "Personal", "Shopping"};
        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categories);
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setAdapter(categoryAdapter);

        String[] priorities = {"High", "Medium", "Low"};
        ArrayAdapter<String> priorityAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, priorities);
        priorityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        prioritySpinner.setAdapter(priorityAdapter);

        // Date picker button
        selectDateButton.setOnClickListener(v -> {
            final Calendar calendar = Calendar.getInstance();
            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);
            int day = calendar.get(Calendar.DAY_OF_MONTH);
            DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                    (view1, selectedYear, selectedMonth, selectedDay) -> {
                        String selectedDate = selectedDay + "-" + (selectedMonth + 1) + "-" + selectedYear;
                        dueDateText.setText(selectedDate);
                    },
                    year, month, day);
            datePickerDialog.show();
        });

        // If editing, pre-fill fields
        if (taskToEdit != null) {
            taskEditText.setText(taskToEdit.getTask());
            dueDateText.setText(taskToEdit.getDueDate());

            int categoryPos = categoryAdapter.getPosition(taskToEdit.getCategory());
            if (categoryPos >= 0) categorySpinner.setSelection(categoryPos);

            int priorityPos = priorityAdapter.getPosition(taskToEdit.getPriority());
            if (priorityPos >= 0) prioritySpinner.setSelection(priorityPos);
        }

        String dialogTitle = (taskToEdit == null) ? "Add New Task" : "Edit Task";
        String positiveButtonText = (taskToEdit == null) ? "Save" : "Update";

        new android.app.AlertDialog.Builder(this)
                .setTitle(dialogTitle)
                .setView(view)
                .setPositiveButton(positiveButtonText, (dialog, which) -> {
                    String taskText = taskEditText.getText().toString().trim();
                    String category = categorySpinner.getSelectedItem().toString();
                    String priority = prioritySpinner.getSelectedItem().toString();
                    String dueDate = dueDateText.getText().toString();

                    if (!taskText.isEmpty()) {
                        if (taskToEdit == null) {
                            // Insert new task
                            ToDoModel task = new ToDoModel();
                            task.setTask(taskText);
                            task.setCategory(category);
                            task.setPriority(priority);
                            task.setDueDate(dueDate);
                            task.setStatus(0);
                            db.insertTask(task);

                            if (!dueDate.isEmpty()) {
                                String[] parts = dueDate.split("-");
                                int day = Integer.parseInt(parts[0]);
                                int month = Integer.parseInt(parts[1]) - 1;  // Months 0-based
                                int year = Integer.parseInt(parts[2]);

                                Calendar dueDateTime = Calendar.getInstance();
                                dueDateTime.set(Calendar.YEAR, year);
                                dueDateTime.set(Calendar.MONTH, month);
                                dueDateTime.set(Calendar.DAY_OF_MONTH, day);
                                dueDateTime.set(Calendar.HOUR_OF_DAY, 11);  // Notify at 9 AM
                                dueDateTime.set(Calendar.MINUTE, 28);
                                dueDateTime.set(Calendar.SECOND, 0);

                                scheduleReminder(task, dueDateTime);
                            }
                        } else {
                            // Update existing task
                            taskToEdit.setTask(taskText);
                            taskToEdit.setCategory(category);
                            taskToEdit.setPriority(priority);
                            taskToEdit.setDueDate(dueDate);
                            db.updateTask(taskToEdit);

                            if (!dueDate.isEmpty()) {
                                String[] parts = dueDate.split("-");
                                int day = Integer.parseInt(parts[0]);
                                int month = Integer.parseInt(parts[1]) - 1;  // Months 0-based
                                int year = Integer.parseInt(parts[2]);

                                Calendar dueDateTime = Calendar.getInstance();
                                dueDateTime.set(Calendar.YEAR, year);
                                dueDateTime.set(Calendar.MONTH, month);
                                dueDateTime.set(Calendar.DAY_OF_MONTH, day);
                                dueDateTime.set(Calendar.HOUR_OF_DAY, 11);  // Notify at 9 AM
                                dueDateTime.set(Calendar.MINUTE, 29);
                                dueDateTime.set(Calendar.SECOND, 0);

                                scheduleReminder(taskToEdit, dueDateTime);
                            }
                        }
                        taskList = db.getAllTasks();
                        Collections.reverse(taskList);
                        applyFilters();
                    } else {
                        // Optional: Toast or error for empty task
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    public void showEditTaskDialog(ToDoModel taskToEdit) {
        View view = getLayoutInflater().inflate(R.layout.new_task, null);

        EditText taskEditText = view.findViewById(R.id.newTaskText);
        Spinner categorySpinner = view.findViewById(R.id.category_spinner);
        Spinner prioritySpinner = view.findViewById(R.id.priority_spinner);
        TextView dueDateText = view.findViewById(R.id.due_date_text);
        Button selectDateButton = view.findViewById(R.id.select_date_button);

        // Pre-fill existing task info
        taskEditText.setText(taskToEdit.getTask());

        // Setup category spinner with existing categories (adjust categories to your app)
        String[] categories = {"Work", "Personal", "Shopping"};
        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categories);
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setAdapter(categoryAdapter);
        int catPos = categoryAdapter.getPosition(taskToEdit.getCategory());
        categorySpinner.setSelection(catPos);

        // Setup priority spinner with existing priorities
        String[] priorities = {"High", "Medium", "Low"};
        ArrayAdapter<String> priorityAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, priorities);
        priorityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        prioritySpinner.setAdapter(priorityAdapter);
        int priPos = priorityAdapter.getPosition(taskToEdit.getPriority());
        prioritySpinner.setSelection(priPos);

        dueDateText.setText(taskToEdit.getDueDate());

        selectDateButton.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            new DatePickerDialog(this,
                    (view1, year, month, dayOfMonth) -> {
                        String date = dayOfMonth + "-" + (month + 1) + "-" + year;
                        dueDateText.setText(date);
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH))
                    .show();
        });

        new AlertDialog.Builder(this)
                .setTitle("Edit Task")
                .setView(view)
                .setPositiveButton("Update", (dialog, which) -> {
                    String updatedTask = taskEditText.getText().toString().trim();
                    String updatedCategory = categorySpinner.getSelectedItem().toString();
                    String updatedPriority = prioritySpinner.getSelectedItem().toString();
                    String updatedDueDate = dueDateText.getText().toString();

                    if (!updatedTask.isEmpty()) {
                        // Update task object
                        taskToEdit.setTask(updatedTask);
                        taskToEdit.setCategory(updatedCategory);
                        taskToEdit.setPriority(updatedPriority);
                        taskToEdit.setDueDate(updatedDueDate);

                        // Update task in database (make sure this method exists)
                        db.updateTask(taskToEdit);

                        // Refresh your list and UI after update (implement accordingly)
                        loadTasks();  // Or whatever method reloads tasks and refreshes adapter
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }



    private void showAddTaskDialog() {
        View view = getLayoutInflater().inflate(R.layout.new_task, null);

        EditText taskEditText = view.findViewById(R.id.newTaskText);
        Spinner categorySpinner = view.findViewById(R.id.category_spinner);
        Spinner prioritySpinner = view.findViewById(R.id.priority_spinner);
        TextView dueDateText = view.findViewById(R.id.due_date_text);
        Button selectDateButton = view.findViewById(R.id.select_date_button);

        // Populate spinners here using ArrayAdapter
        String[] categories = {"Work", "Personal", "Shopping"};
        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categories);
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setAdapter(categoryAdapter);

        // Populate Priority Spinner
        String[] priorities = {"High", "Medium", "Low"};
        ArrayAdapter<String> priorityAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, priorities);
        priorityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        prioritySpinner.setAdapter(priorityAdapter);

        // You can also add date picker logic on selectDateButton click
        selectDateButton.setOnClickListener(v -> {
            final Calendar calendar = Calendar.getInstance();
            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);
            int day = calendar.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                    (view1, selectedYear, selectedMonth, selectedDay) -> {
                        // Format date as "dd-MM-yyyy"
                        String selectedDate = selectedDay + "-" + (selectedMonth + 1) + "-" + selectedYear;
                        dueDateText.setText(selectedDate);
                    },
                    year, month, day);

            datePickerDialog.show();
        });


        new android.app.AlertDialog.Builder(this)
                .setTitle("Add New Task")
                .setView(view)
                .setPositiveButton("Save", (dialog, which) -> {
                    String taskText = taskEditText.getText().toString().trim();
                    String category = categorySpinner.getSelectedItem().toString();
                    String priority = prioritySpinner.getSelectedItem().toString();
                    String dueDate = dueDateText.getText().toString();

                    if (!taskText.isEmpty()) {
                        ToDoModel task = new ToDoModel();
                        task.setTask(taskText);
                        task.setCategory(category);
                        task.setPriority(priority);
                        task.setDueDate(dueDate);
                        task.setStatus(0);

                        db.insertTask(task);
                        taskList = db.getAllTasks();
                        Collections.reverse(taskList);
                        applyFilters();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }


    private void showFilterDialog() {
        String[] options = {"Category", "Priority", "Show All"};
        new android.app.AlertDialog.Builder(this)
                .setTitle("Filter By")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            showCategoryFilterDialog();
                            break;
                        case 1:
                            showPriorityFilterDialog();
                            break;
                        case 2:
                            // Reset filters
                            currentCategoryFilter = null;
                            currentPriorityFilter = null;
                            tasksAdapter.setTasks(taskList);
                            updateActiveFiltersText();
                            break;
                    }
                })
                .show();
    }

    private void showCategoryFilterDialog() {
        List<String> categories = db.getAllCategories(); // Make sure this method is implemented in DatabaseHandler
        if (categories.isEmpty()) {
            // Handle case where no categories exist
            new android.app.AlertDialog.Builder(this)
                    .setTitle("No Categories")
                    .setMessage("There are no categories available.")
                    .setPositiveButton("OK", null)
                    .show();
            return;
        }
        String[] categoryArray = categories.toArray(new String[0]);
        new android.app.AlertDialog.Builder(this)
                .setTitle("Select Category")
                .setItems(categoryArray, (dialog, which) -> {
                    currentCategoryFilter = categoryArray[which];
                    applyFilters();
                })
                .show();
    }

    private void showPriorityFilterDialog() {
        String[] priorities = {"High", "Medium", "Low"};
        new android.app.AlertDialog.Builder(this)
                .setTitle("Select Priority")
                .setItems(priorities, (dialog, which) -> {
                    currentPriorityFilter = priorities[which];
                    applyFilters();
                })
                .show();
    }

    // Combine filters for Category and Priority
    private void applyFilters() {
        List<ToDoModel> filtered = new ArrayList<>();
        for (ToDoModel task : taskList) {
            boolean matchesCategory = (currentCategoryFilter == null) || task.getCategory().equalsIgnoreCase(currentCategoryFilter);
            boolean matchesPriority = (currentPriorityFilter == null) || task.getPriority().equalsIgnoreCase(currentPriorityFilter);
            if (matchesCategory && matchesPriority) {
                filtered.add(task);
            }
        }
        tasksAdapter.setTasks(filtered);
        updateActiveFiltersText();
    }

    private void updateActiveFiltersText() {
        StringBuilder sb = new StringBuilder("Filters: ");
        boolean hasFilter = false;

        if (currentCategoryFilter != null && !currentCategoryFilter.isEmpty()) {
            sb.append("Category = ").append(currentCategoryFilter);
            hasFilter = true;
        }
        if (currentPriorityFilter != null && !currentPriorityFilter.isEmpty()) {
            if (hasFilter) sb.append(", ");
            sb.append("Priority = ").append(currentPriorityFilter);
            hasFilter = true;
        }
        if (!hasFilter) {
            sb.append("None");
        }
        activeFiltersText.setText(sb.toString());
    }

    @Override
    public void handleDialogClose(DialogInterface dialog) {
        taskList = db.getAllTasks();
        Collections.reverse(taskList);
        applyFilters();  // instead of directly setting all tasks, apply current filters
        tasksAdapter.notifyDataSetChanged();
        Log.d("MainActivity", "Tasks refreshed: " + taskList.size());
    }

    public void loadTasks() {
        taskList = db.getAllTasks();      // Load tasks from DB
        Collections.reverse(taskList);    // Optional: reverse list if needed
        applyFilters();                   // Apply current filters (or just update adapter if no filters)
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "TaskReminders";
            String description = "Channel for Task Due Reminders";
            int importance = NotificationManager.IMPORTANCE_HIGH;

            NotificationChannel channel = new NotificationChannel("task_reminder", name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void scheduleReminder(ToDoModel task, Calendar dueDateTime) {
        try {
            Intent intent = new Intent(getApplicationContext(), ReminderBroadcast.class);
            intent.putExtra("task", task.getTask());
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    getApplicationContext(),
                    task.getId(),
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, dueDateTime.getTimeInMillis(), pendingIntent);
        } catch (Exception e) {
            Log.e("ReminderError", "Failed to schedule reminder: " + e.getMessage());
        }
    }





}
