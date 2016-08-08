package io.github.krtkush.marsexplorer.Main;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.github.krtkush.marsexplorer.GeneralConstants;
import io.github.krtkush.marsexplorer.R;
import timber.log.Timber;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

public class MainActivity extends AppCompatActivity {

    @BindView(R.id.maxMarsTemperatureTextView) TextView maxTemperatureTextView;
    @BindView(R.id.minMarsTemperatureTextView) TextView minTemperatureTextView;
    @BindView(R.id.currentSol) TextView currentSolTextView;
    @BindView(R.id.pressureTextView) TextView atmosphericPressureTextView;

    private MainActivityPresenterInteractor presenterInteractor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialise butterknife, timber and the presenter layer
        ButterKnife.bind(MainActivity.this);
        Timber.tag(MainActivity.this.getClass().getSimpleName());
        presenterInteractor = new MainActivityPresenterLayer(this);

        // Send request to fetch Mars weather data
        presenterInteractor.getMarsWeather();

        // Send request to get max SOL for each rover
        presenterInteractor.getMaxSol(GeneralConstants.Curiosity);
        presenterInteractor.getMaxSol(GeneralConstants.Spirit);
        presenterInteractor.getMaxSol(GeneralConstants.Opportunity);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Check internet connectivity
        presenterInteractor.checkInternetConnectivity();
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }

    @Override
    protected void onStop() {
        super.onStop();

        presenterInteractor.unsubscribeMarsWeatherRequest();
        presenterInteractor.unsubscribeMaxSolRequest();
    }

    /**
     * Method to set the temperature as provided by the report
     * @param maxMarsTemperature
     * @param minMarsTemperature
     * @param currentSol
     * @param atmosphericPressure
     */
    protected void setMarsWeather(String maxMarsTemperature,
                                  String minMarsTemperature,
                                  String currentSol,
                                  String atmosphericPressure) {

        currentSolTextView.setText(currentSol);
        maxTemperatureTextView.setText(maxMarsTemperature);
        minTemperatureTextView.setText(minMarsTemperature);
        atmosphericPressureTextView.setText(atmosphericPressure);
    }

    @OnClick(R.id.goToCuriosity)
    public void goToCuriosity(View view) {
        presenterInteractor.goToRoverSection(GeneralConstants.Curiosity);
    }

    protected void showToast(String message) {

        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }
}
