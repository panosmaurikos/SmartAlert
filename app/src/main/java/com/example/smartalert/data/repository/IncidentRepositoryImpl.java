    package com.example.smartalert.data.repository;

    import com.example.smartalert.data.remote.IncidentRemoteDataSource;
    import com.example.smartalert.domain.model.Incident;
    import com.example.smartalert.domain.repository.IncidentRepository;
    import com.google.android.gms.tasks.OnCompleteListener;
    import com.google.firebase.firestore.QuerySnapshot;

    public class IncidentRepositoryImpl implements IncidentRepository {
        private IncidentRemoteDataSource remoteDataSource;

        public IncidentRepositoryImpl() {
            remoteDataSource = new IncidentRemoteDataSource();
        }

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
