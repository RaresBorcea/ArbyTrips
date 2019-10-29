package com.arbytek.trips.datePicker;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.widget.DatePicker;

import androidx.fragment.app.DialogFragment;

import com.arbytek.trips.ManageTripActivity;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * Constructs a new DialogFragment for the start date of a trip
 */

public class StartDateFragment extends DialogFragment {

    Calendar mCalendar = Calendar.getInstance();
    private StartDateDialogListener mDateListener;
    private final static String TAG = "StartDateFragment";

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if (ManageTripActivity.getStartDate() != null) {
            setDate(ManageTripActivity.getStartDate());
        }
        int year = mCalendar.get(Calendar.YEAR);
        int month = mCalendar.get(Calendar.MONTH);
        int day = mCalendar.get(Calendar.DAY_OF_MONTH);

        return new DatePickerDialog(getActivity(), dateSetListener, year, month, day);
    }

    private DatePickerDialog.OnDateSetListener dateSetListener =
            new DatePickerDialog.OnDateSetListener() {
                public void onDateSet(DatePicker view, int year, int month, int day) {
                    setDate("" + day + "/" + (month + 1) + "/" + year);
                    Date mDate = mCalendar.getTime();
                    SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
                    mDateListener.applyStartDate(formatter.format(mDate));
                    // Set selected date in ManageTripActivity static fields
                    ManageTripActivity.setStartDate(formatter.format(mDate));
                }
            };

    public void setDate(String date) {
        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
        Date mDate;

        try {
            mDate = formatter.parse(date);
            mCalendar.setTime(mDate);
        } catch (ParseException e) {
            e.printStackTrace();
            Log.d(TAG, "Error parsing set date");
        }
    }

    /**
     * Set date on ManageTripActivity button
     */
    public interface StartDateDialogListener {
        void applyStartDate(String startDate);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        try {
            mDateListener = (StartDateDialogListener) context;
        } catch (ClassCastException e) {
            e.printStackTrace();
        }
    }
}