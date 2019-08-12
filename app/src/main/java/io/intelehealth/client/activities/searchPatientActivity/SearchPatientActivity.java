package io.intelehealth.client.activities.searchPatientActivity;

import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.provider.SearchRecentSuggestions;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import io.intelehealth.client.R;
import io.intelehealth.client.activities.homeActivity.HomeActivity;
import io.intelehealth.client.app.AppConstants;
import io.intelehealth.client.database.dao.ProviderDAO;
import io.intelehealth.client.models.dto.PatientDTO;
import io.intelehealth.client.utilities.Logger;
import io.intelehealth.client.utilities.SessionManager;
import io.intelehealth.client.utilities.StringUtils;
import io.intelehealth.client.utilities.exception.DAOException;

public class SearchPatientActivity extends AppCompatActivity {
    SearchView searchView;
    String query;
    private SearchPatientAdapter recycler;
    RecyclerView recyclerView;
    SessionManager sessionManager = null;
    TextView msg;
    AlertDialog.Builder dialogBuilder;
    private String TAG = SearchPatientActivity.class.getSimpleName();
    private SQLiteDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_patient);
        Toolbar toolbar = findViewById(R.id.toolbar);

//        Drawable drawable = ContextCompat.getDrawable(getApplicationContext(),
//                R.drawable.ic_sort_white_24dp);
//        toolbar.setOverflowIcon(drawable);

        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        // Get the intent, verify the action and get the query
        sessionManager = new SessionManager(this);
        db = AppConstants.inteleHealthDatabaseHelper.getWriteDb();
        msg = findViewById(R.id.textviewmessage);
        recyclerView = findViewById(R.id.recycle);
        Intent intent = getIntent();
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            query = intent.getStringExtra(SearchManager.QUERY);
            SearchRecentSuggestions suggestions = new SearchRecentSuggestions(this, SearchSuggestionProvider.AUTHORITY, SearchSuggestionProvider.MODE);
            suggestions.saveRecentQuery(query, null);
//            searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
//                @Override
//                public boolean onQueryTextSubmit(String s) {
//                    return false;
//                }
//
//                @Override
//                public boolean onQueryTextChange(String s) {
//                    Log.d("Hack", "in query text change");
//                SearchRecentSuggestions suggestions = new SearchRecentSuggestions(SearchPatientActivity.this,
//                        SearchSuggestionProvider.AUTHORITY, SearchSuggestionProvider.MODE);
//                suggestions.clearHistory();
//                doQuery(s);
//                return true;
////                    return false;
//                }
//            });
            if (sessionManager.isPullSyncFinished()) {
                msg.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);
                doQuery(query);
            }

        } else {
            SearchRecentSuggestions suggestions = new SearchRecentSuggestions(this,
                    SearchSuggestionProvider.AUTHORITY, SearchSuggestionProvider.MODE);
            suggestions.saveRecentQuery(query, null);
            if (sessionManager.isPullSyncFinished()) {
                msg.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);
                firstQuery();
            }

        }


    }

    private void doQuery(String query) {
        try {
            recycler = new SearchPatientAdapter(getQueryPatients(query), SearchPatientActivity.this);
//            Log.i("db data", "" + getQueryPatients(query));
            RecyclerView.LayoutManager reLayoutManager = new LinearLayoutManager(getApplicationContext());
            recyclerView.setLayoutManager(reLayoutManager);
            recyclerView.setItemAnimator(new DefaultItemAnimator());
            recyclerView.setAdapter(recycler);

        } catch (Exception e) {
            Logger.logE("doquery", "doquery", e);
        }
    }

    private void firstQuery() {
        try {
            getAllPatientsFromDB();

            recycler = new SearchPatientAdapter(getAllPatientsFromDB(), SearchPatientActivity.this);


//            Log.i("db data", "" + getAllPatientsFromDB());
            RecyclerView.LayoutManager reLayoutManager = new LinearLayoutManager(getApplicationContext());
            recyclerView.setLayoutManager(reLayoutManager);
            recyclerView.setItemAnimator(new DefaultItemAnimator());
            recyclerView.setAdapter(recycler);

        } catch (Exception e) {
            Logger.logE("firstquery", "exception", e);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the options menu from XMLz
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_search, menu);
//        inflater.inflate(R.menu.today_filter, menu);
        // Get the SearchView and set the searchable configuration
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        searchView = (SearchView) menu.findItem(R.id.action_search).getActionView();
        // Assumes current activity is the searchable activity
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
//        searchView.setIconifiedByDefault(false); // Do not iconify the widget; expand it by default
        //searchView.setMaxWidth(Integer.MAX_VALUE);

//        searchView.setFocusable(true);
//        searchView.requestFocus();

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                Log.d("Hack", "in query text change");
                SearchRecentSuggestions suggestions = new SearchRecentSuggestions(SearchPatientActivity.this,
                        SearchSuggestionProvider.AUTHORITY, SearchSuggestionProvider.MODE);
                suggestions.clearHistory();
                query = newText;
                doQuery(newText);
                return true;
            }
        });


        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.summary_endAllVisit:
                endAllVisit();

            case R.id.action_filter:
                //alert box.
                displaySingleSelectionDialog();    //function call
            case R.id.action_search:



            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * This method is called when no search result is found for patient.
     *
     * @param lvItems variable of type ListView
     * @param query   variable of type String
     */
    public void noneFound(ListView lvItems, String query) {
        ArrayAdapter<String> searchAdapter = new ArrayAdapter<>(this,
                R.layout.list_item_search,
                R.id.list_item_head, new ArrayList<String>());
        String errorMessage = getString(R.string.alert_none_found).replace("_", query);
        searchAdapter.add(errorMessage);
        lvItems.setAdapter(searchAdapter);
    }

    public List<PatientDTO> getAllPatientsFromDB() {
        List<PatientDTO> modelList = new ArrayList<PatientDTO>();
        String table = "tbl_patient";
        final Cursor searchCursor = db.rawQuery("SELECT * FROM " + table +
                " ORDER BY first_name ASC", null);

        if (searchCursor.moveToFirst()) {
            do {
                PatientDTO model = new PatientDTO();
                model.setOpenmrsId(searchCursor.getString(searchCursor.getColumnIndexOrThrow("openmrs_id")));
                model.setFirstname(searchCursor.getString(searchCursor.getColumnIndexOrThrow("first_name")));
                model.setLastname(searchCursor.getString(searchCursor.getColumnIndexOrThrow("last_name")));
                model.setOpenmrsId(searchCursor.getString(searchCursor.getColumnIndexOrThrow("openmrs_id")));
                model.setUuid(searchCursor.getString(searchCursor.getColumnIndexOrThrow("uuid")));
                modelList.add(model);
            } while (searchCursor.moveToNext());
        }
        searchCursor.close();


        //  Log.d("student data", modelList.toString());


        return modelList;

    }

    private void endAllVisit() {

        int failedUploads = 0;

        String query = "SELECT tbl_visit.patientuuid, tbl_visit.enddate, tbl_visit.uuid," +
                "tbl_patient.first_name, tbl_patient.middle_name, tbl_patient.last_name FROM tbl_visit, tbl_patient WHERE" +
                " tbl_visit.patientuid = tbl_patient.uuid AND tbl_visit.enddate IS NULL OR tbl_visit.enddate = ''";

        final Cursor cursor = db.rawQuery(query, null);

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                do {
                    boolean result = endVisit(
                            cursor.getString(cursor.getColumnIndexOrThrow("patientuuid")),
                            cursor.getString(cursor.getColumnIndexOrThrow("first_name")) + " " +
                                    cursor.getString(cursor.getColumnIndexOrThrow("last_name")),
                            cursor.getString(cursor.getColumnIndexOrThrow("uuid"))
                    );
                    if (!result) failedUploads++;
                } while (cursor.moveToNext());
            }
        }
        cursor.close();

        if (failedUploads == 0) {
            Intent intent = new Intent(this, HomeActivity.class);
            startActivity(intent);
        } else {
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
            alertDialogBuilder.setMessage("Unable to end " + failedUploads +
                    " visits.Please upload visit before attempting to end the visit.");
            alertDialogBuilder.setNeutralButton(R.string.generic_ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            AlertDialog alertDialog = alertDialogBuilder.create();
            alertDialog.show();
        }

    }

    private boolean endVisit(String patientUuid, String patientName, String visitUUID) {

        return visitUUID != null;

    }

    private void displaySingleSelectionDialog() {

        ProviderDAO providerDAO = new ProviderDAO();
        ArrayList selectedItems = new ArrayList<>();
        String[] creator_names = null;
        String[] creator_uuid = null;
        try {
            creator_names = providerDAO.getProvidersList().toArray(new String[0]);
            creator_uuid = providerDAO.getProvidersUuidList().toArray(new String[0]);
        } catch (DAOException e) {
            e.printStackTrace();
        }
//        boolean[] checkedItems = {false, false, false, false};
        // ngo_numbers = getResources().getStringArray(R.array.ngo_numbers);
        dialogBuilder = new AlertDialog.Builder(SearchPatientActivity.this);
        dialogBuilder.setTitle("Filter by Creator");

        String[] finalCreator_names = creator_names;
        String[] finalCreator_uuid = creator_uuid;
        dialogBuilder.setMultiChoiceItems(creator_names, null, new DialogInterface.OnMultiChoiceClickListener() {


            @Override
            public void onClick(DialogInterface dialogInterface, int which, boolean isChecked) {
                Logger.logD(TAG, "multichoice" + which + isChecked);
                if (isChecked) {
                    // If the user checked the item, add it to the selected items
                    selectedItems.add(finalCreator_uuid[which]);
                    Logger.logD(TAG, finalCreator_names[which] + finalCreator_uuid[which]);

                } else if (selectedItems.contains(which)) {
                    // Else, if the item is already in the array, remove it
                    selectedItems.remove(finalCreator_uuid[which]);
                    Logger.logD(TAG, finalCreator_names[which] + finalCreator_uuid[which]);
                }

            }
        });

        dialogBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                //display filter query code on list menu
                Logger.logD(TAG, "onclick" + i);
                doQueryWithProviders(query, selectedItems);
//                select distinct a.uuid,c.first_name,c.middle_name,c.last_name,c.openmrs_id,c.phone_number,c.date_of_birth from tbl_visit a,tbl_encounter b ,tbl_patient c where b.visituuid=a.uuid and b.provider_uuid in ('163b48e5-26fb-40c1-8d94-a6c873dd2869') and a.patientuuid=c.uuid and a.enddate is null order by c.first_name
            }
        });

        dialogBuilder.setNegativeButton("Cancel", null);

        dialogBuilder.show();

    }

    public List<PatientDTO> getQueryPatients(String query) {
        String search = query.trim();
        List<PatientDTO> modelList = new ArrayList<PatientDTO>();
        SQLiteDatabase db = AppConstants.inteleHealthDatabaseHelper.getWritableDatabase();
        String table = "tbl_patient";
        final Cursor searchCursor = db.rawQuery("SELECT * FROM " + table +
                " WHERE first_name LIKE " + "'" + search +
                "%' OR last_name LIKE '" + search +
                "%' OR openmrs_id LIKE '" + search +
                "%' OR middle_name LIKE '" + search + "%' " +
                "ORDER BY first_name ASC", null);

        if (searchCursor.moveToFirst()) {
            do {
                PatientDTO model = new PatientDTO();
                model.setOpenmrsId(searchCursor.getString(searchCursor.getColumnIndexOrThrow("openmrs_id")));
                model.setFirstname(searchCursor.getString(searchCursor.getColumnIndexOrThrow("first_name")));
                model.setLastname(searchCursor.getString(searchCursor.getColumnIndexOrThrow("last_name")));
                model.setOpenmrsId(searchCursor.getString(searchCursor.getColumnIndexOrThrow("openmrs_id")));
                model.setMiddlename(searchCursor.getString(searchCursor.getColumnIndexOrThrow("middle_name")));
                model.setUuid(searchCursor.getString(searchCursor.getColumnIndexOrThrow("uuid")));

                modelList.add(model);
            } while (searchCursor.moveToNext());
        }
        return modelList;

    }

    private void doQueryWithProviders(String querytext, List<String> providersuuids) {
        if (null != querytext && !querytext.isEmpty()) {
            String search = querytext.trim();
            List<PatientDTO> modelList = new ArrayList<PatientDTO>();
            String query =
                    "select  *  " +
                            "from tbl_encounter a ,tbl_patient b " +
                            "left join tbl_patient_attribute c on c.patientuuid=b.uuid " +
                            " WHERE first_name LIKE " + "'" + search +
                            "%' OR last_name LIKE '" + search +
                            "%' OR openmrs_id LIKE '" + search +
                            "%' OR middle_name LIKE '" + search + "%' " +
                            "AND a.provider_uuid in ('" + StringUtils.convertUsingStringBuilder(providersuuids) + "')  " +
                            "group by b.uuid order by b.uuid ASC";
            Logger.logD(TAG, query);
            final Cursor cursor = db.rawQuery(query, null);

            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    do {
                        PatientDTO model = new PatientDTO();
                        model.setOpenmrsId(cursor.getString(cursor.getColumnIndexOrThrow("openmrs_id")));
                        model.setFirstname(cursor.getString(cursor.getColumnIndexOrThrow("first_name")));
                        model.setLastname(cursor.getString(cursor.getColumnIndexOrThrow("last_name")));
                        model.setOpenmrsId(cursor.getString(cursor.getColumnIndexOrThrow("openmrs_id")));
                        model.setMiddlename(cursor.getString(cursor.getColumnIndexOrThrow("middle_name")));
                        model.setUuid(cursor.getString(cursor.getColumnIndexOrThrow("uuid")));

                        modelList.add(model);

                    } while (cursor.moveToNext());
                }
            }
            cursor.close();

            try {
                recycler = new SearchPatientAdapter(modelList, SearchPatientActivity.this);
//            Log.i("db data", "" + getQueryPatients(query));
                RecyclerView.LayoutManager reLayoutManager = new LinearLayoutManager(getApplicationContext());
                recyclerView.setLayoutManager(reLayoutManager);
                recyclerView.setItemAnimator(new DefaultItemAnimator());
                recyclerView.setAdapter(recycler);

            } catch (Exception e) {
                Logger.logE("doquery", "doquery", e);
            }
        }

    }

    @Override
    protected void onStop() {
        super.onStop();
    }
}



