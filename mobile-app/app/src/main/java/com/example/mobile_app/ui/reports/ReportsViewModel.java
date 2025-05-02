package com.example.mobile_app.ui.reports;

import static com.example.mobile_app.ui.api.BackendApiConfig.currentUrl;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.io.IOException;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ReportsViewModel extends ViewModel {
    private final MutableLiveData<List<Report>> reports = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> deleteSuccess = new MutableLiveData<>();

    public LiveData<List<Report>> getReports() {
        return reports;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public void fetchReports() {
        isLoading.postValue(true);
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(currentUrl + "/api/reports")
                .build();

        client.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(@NonNull okhttp3.Call call, @NonNull IOException e) {
                isLoading.postValue(false);
            }

            @Override
            public void onResponse(@NonNull okhttp3.Call call, @NonNull okhttp3.Response response) throws IOException {
                isLoading.postValue(false);
                if (response.isSuccessful() && response.body() != null) {
                    String responseData = response.body().string();
                    if (responseData.isEmpty()) {
                        reports.postValue(null);
                    } else {
                        List<Report> reportList = Report.mapJsonToReports(responseData);
                        reports.postValue(reportList);
                    }
                } else {
                    reports.postValue(null);
                }
            }
        });
    }

    public LiveData<Boolean> getDeleteSuccess() {
        return deleteSuccess;
    }

    public void deleteAllReports() {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(currentUrl + "/api/deleteReports")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                deleteSuccess.postValue(false);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                if (response.isSuccessful()) {
                    deleteSuccess.postValue(true);
                    fetchReports();
                } else {
                    deleteSuccess.postValue(false);
                }
            }
        });
    }
}