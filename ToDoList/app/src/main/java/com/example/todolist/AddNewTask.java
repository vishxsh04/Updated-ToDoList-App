package com.example.todolist;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import com.example.todolist.Model.ToDoModel;
import com.example.todolist.Utils.DatabaseHandler;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.Calendar;

public class AddNewTask extends BottomSheetDialogFragment {

    public static final String TAG = "ActionBottomDialog";

    private EditText newTaskText;
    private Button newTaskSaveButton, selectDateButton;
    private TextView dueDateText;
    private Spinner categorySpinner, prioritySpinner;

    private DatabaseHandler db;
    private boolean isUpdate = false;
    private Bundle bundle;

    public static AddNewTask newInstance() {
        return new AddNewTask();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(STYLE_NORMAL, R.style.DialogStyle);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.new_task, container, false);
        getDialog().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize UI components
        newTaskText = view.findViewById(R.id.newTaskText);
//        newTaskSaveButton = view.findViewById(R.id.newTaskSaveButton);
        categorySpinner = view.findViewById(R.id.category_spinner);
        prioritySpinner = view.findViewById(R.id.priority_spinner);
        dueDateText = view.findViewById(R.id.due_date_text);
        selectDateButton = view.findViewById(R.id.select_date_button);

        db = new DatabaseHandler(getActivity());
        db.openDatabase();

        // Set up spinner adapters
        ArrayAdapter<CharSequence> categoryAdapter = ArrayAdapter.createFromResource(
                getContext(), R.array.category_array, android.R.layout.simple_spinner_item);
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setAdapter(categoryAdapter);

        ArrayAdapter<CharSequence> priorityAdapter = ArrayAdapter.createFromResource(
                getContext(), R.array.priority_array, android.R.layout.simple_spinner_item);
        priorityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        prioritySpinner.setAdapter(priorityAdapter);

        // Date picker setup
        selectDateButton.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            DatePickerDialog datePickerDialog = new DatePickerDialog(getContext(),
                    (view1, year, month, dayOfMonth) -> {
                        String date = dayOfMonth + "/" + (month + 1) + "/" + year;
                        dueDateText.setText(date);
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH));
            datePickerDialog.show();
        });

        // Check if this is an update
        bundle = getArguments();
        if (bundle != null) {
            isUpdate = true;

            String task = bundle.getString("task");
            String category = bundle.getString("category");
            String priority = bundle.getString("priority");
            String dueDate = bundle.getString("dueDate");

            newTaskText.setText(task);

            if (category != null) {
                int categoryPosition = ((ArrayAdapter) categorySpinner.getAdapter()).getPosition(category);
                categorySpinner.setSelection(categoryPosition);
            }

            if (priority != null) {
                int priorityPosition = ((ArrayAdapter) prioritySpinner.getAdapter()).getPosition(priority);
                prioritySpinner.setSelection(priorityPosition);
            }

            if (dueDate != null && !dueDate.isEmpty()) {
                dueDateText.setText(dueDate);
            }

            if (task != null && !task.isEmpty()) {
                newTaskSaveButton.setTextColor(ContextCompat.getColor(getContext(), R.color.colorPrimaryDark));
            }
        }

        newTaskText.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.toString().isEmpty()) {
                    newTaskSaveButton.setEnabled(false);
                    newTaskSaveButton.setTextColor(Color.GRAY);
                } else {
                    newTaskSaveButton.setEnabled(true);
                    newTaskSaveButton.setTextColor(ContextCompat.getColor(getContext(), R.color.colorPrimaryDark));
                }
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });

        newTaskSaveButton.setOnClickListener(v -> {
            String text = newTaskText.getText().toString();
            String category = categorySpinner.getSelectedItem().toString();
            String priority = prioritySpinner.getSelectedItem().toString();
            String dueDate = dueDateText.getText().toString();

            ToDoModel task = new ToDoModel();
            task.setTask(text);
            task.setCategory(category);
            task.setPriority(priority);
            task.setDueDate(dueDate);
            task.setStatus(0);

            if (isUpdate) {
                task.setId(bundle.getInt("id"));
                db.updateTask(task);
            } else {
                db.insertTask(task);
                Toast.makeText(getContext(), "Task added successfully!", Toast.LENGTH_SHORT).show();  // ✅ Add this line
            }

            dismiss();
        });

    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        android.util.Log.d("AddNewTask", "Dialog dismissed");
        Activity activity = getActivity();
        if (activity instanceof DialogCloseListener) {
            ((DialogCloseListener) activity).handleDialogClose(dialog);
        }
    }

}
