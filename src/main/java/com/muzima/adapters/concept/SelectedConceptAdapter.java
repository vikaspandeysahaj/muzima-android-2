/*
 * Copyright (c) 2014. The Trustees of Indiana University.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license with additional
 * healthcare disclaimer. If the user is an entity intending to commercialize any application
 * that uses this code in a for-profit venture, please contact the copyright holder.
 */

/**
 * Copyright 2012 Muzima Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.muzima.adapters.concept;

import android.os.AsyncTask;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.adapters.ListAdapter;
import com.muzima.api.model.Concept;
import com.muzima.controller.ConceptController;

import java.util.Arrays;
import java.util.List;

/**
 * Responsible to display Concepts in the Settings page.
 */
public class SelectedConceptAdapter extends ListAdapter<Concept> {
    private final String TAG = SelectedConceptAdapter.class.getSimpleName();
    protected ConceptController conceptController;

    public SelectedConceptAdapter(MuzimaApplication context, int textViewResourceId, ConceptController conceptController) {
        super(context, textViewResourceId);
        this.conceptController = conceptController;
    }

    public boolean doesConceptAlreadyExist(Concept selectedConcept) {
        try {
            return conceptController.getConcepts().contains(selectedConcept);
        } catch (ConceptController.ConceptFetchException e) {
            Log.e(TAG, "Error while loading concepts", e);
        }
        return false;
    }

    private class ViewHolder {
        private TextView name;
        private TextView synonyms;
        private ImageButton deleteButton;

        private ViewHolder(View conceptView) {
            name = (TextView) conceptView.findViewById(R.id.concept_name);
            synonyms = (TextView) conceptView.findViewById(R.id.concept_synonyms);
            deleteButton = (ImageButton) conceptView.findViewById(R.id.delete_concept_btn);
        }

        private View.OnClickListener deleteConceptListener(final int position) {
            return new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    view.setClickable(false);
                    remove(getItem(position));
                }
            };
        }
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            LayoutInflater layoutInflater = LayoutInflater.from(getContext());
            convertView = layoutInflater.inflate(R.layout.item_concept_list, parent, false);
            holder = new ViewHolder(convertView);
            convertView.setTag(holder);
        }
        holder = (ViewHolder) convertView.getTag();
        Concept concept = getItem(position);
        if (concept != null) {
            holder.name.setText(concept.getName());
            holder.synonyms.setText(concept.getSynonyms());
            holder.deleteButton.setOnClickListener(holder.deleteConceptListener(position));
        }
        return convertView;
    }

    @Override
    public void remove(Concept concept) {
        super.remove(concept);
        try {
            conceptController.deleteConcept(concept);
        } catch (ConceptController.ConceptDeleteException e) {
            Log.e(TAG, "Error while deleting the concept", e);
        }
    }

    @Override
    public void reloadData() {
        new BackgroundSaveAndQueryTask().execute();
    }

    /**
     * Responsible to save the concept into DB on selection from AutoComplete. And also fetches to Concepts from DB to display in the page.
     */
    public class BackgroundSaveAndQueryTask extends AsyncTask<Concept, Void, List<Concept>> {

        @Override
        protected List<Concept> doInBackground(Concept... concepts) {
            List<Concept> selectedConcepts = null;
            List<Concept> conceptList = Arrays.asList(concepts);
            try {
                if (concepts.length > 0) {
                    // Called with Concept which is selected in the AutoComplete menu.
                    conceptController.saveConcepts(conceptList);
                }
                selectedConcepts = conceptController.getConcepts();
            } catch (ConceptController.ConceptSaveException e) {
                Log.w(TAG, "Exception occurred while saving concept to local data repository!", e);
            } catch (ConceptController.ConceptFetchException e) {
                Log.w(TAG, "Exception occurred while fetching concepts from local data repository!", e);
            }
            return selectedConcepts;
        }

        @Override
        protected void onPostExecute(List<Concept> concepts) {
            if (concepts == null) {
                Toast.makeText(getContext(), "Something went wrong while fetching concepts from local repo", Toast.LENGTH_SHORT).show();
                return;
            }
            clear();
            addAll(concepts);
            notifyDataSetChanged();
        }
    }

    public void addConcept(Concept concept) {
        new BackgroundSaveAndQueryTask().execute(concept);
    }
}
