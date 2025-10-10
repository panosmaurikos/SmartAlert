    package com.example.smartalert.data.repository;

    import com.example.smartalert.data.remote.IncidentRemoteDataSource;
    import com.example.smartalert.domain.model.Incident;
    import com.example.smartalert.domain.repository.IncidentRepository;
    import com.google.android.gms.tasks.OnCompleteListener;
    import com.google.firebase.firestore.QuerySnapshot;

    /**
     * Serves as the main data access point for incident related operations
     * Delegates actual data operations to the remote data source
     */

    public class IncidentRepositoryImpl implements IncidentRepository {
        // Reference to the remote data source that handles Firebase communication
        private IncidentRemoteDataSource remoteDataSource;
        /**
         * Constructor initializes the remote data source
         * Creates a new instance of IncidentRemoteDataSource for database operations
         */
        public IncidentRepositoryImpl() {
            remoteDataSource = new IncidentRemoteDataSource();
        }
        // Submits a new incident to the database
        @Override
        public void submitIncident(Incident incident, OnCompleteListener<Void> listener) {
            remoteDataSource.submitIncident(incident, listener);
        }

        @Override
        public void getUserIncidents(String userId, OnCompleteListener<QuerySnapshot> listener) {
            remoteDataSource.getUserIncidents(userId, listener);
        }

        @Override
        public void getAllIncidents(OnCompleteListener<QuerySnapshot> listener) {
            remoteDataSource.getAllIncidents(listener);
        }
    }
