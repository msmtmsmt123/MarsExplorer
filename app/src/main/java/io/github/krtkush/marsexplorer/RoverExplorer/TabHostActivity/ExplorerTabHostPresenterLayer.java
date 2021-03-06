package io.github.krtkush.marsexplorer.RoverExplorer.TabHostActivity;

import android.content.ComponentName;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.customtabs.CustomTabsClient;
import android.support.customtabs.CustomTabsIntent;
import android.support.customtabs.CustomTabsServiceConnection;
import android.support.customtabs.CustomTabsSession;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.view.MenuItem;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import io.github.krtkush.marsexplorer.GeneralConstants;
import io.github.krtkush.marsexplorer.MarsExplorerApplication;
import io.github.krtkush.marsexplorer.R;
import io.github.krtkush.marsexplorer.RESTClients.DataModels.PhotosJsonDataModels.PhotosResultDM;
import io.github.krtkush.marsexplorer.RoverExplorer.ExplorerFragment.RoverExplorerFragment;
import io.github.krtkush.marsexplorer.RoverExplorer.RoverExplorerConstants;
import io.github.krtkush.marsexplorer.UtilityMethods;
import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import timber.log.Timber;

/**
 * Created by kartikeykushwaha on 01/09/16.
 */
public class ExplorerTabHostPresenterLayer implements ExplorerTabHostPresenterInteractor {

    private RoverExplorerTabHostActivity activity;
    private String roverName;
    private String roverSol;
    // Variable to keep track of how many SOLs have had their respective tabs added
    // to the viewpager.
    private int roverSolTracker;
    // Viewpager and TabLayout instance. Only used and initiated if maxSol is not passed
    // from previous activity.
    private ViewPager viewPager = null;
    private TabLayout tabLayout = null;

    // Variables for CustomTabs.
    private CustomTabsIntent customTabsIntent;
    private CustomTabsClient customTabsClient;
    private CustomTabsSession customTabsSession;
    private String OPPORTUNITY_WIKIPEDIA_PAGE = "https://en.wikipedia.org/wiki/Opportunity_(rover)";
    private String SPIRIT_WIKIPEDIA_PAGE = "https://en.wikipedia.org/wiki/Spirit_(rover)";
    private String CURIOSITY_WIKIPEDIA_PAGE = "https://en.wikipedia.org/wiki/Curiosity_(rover)";
    // Keep track if the CustomTab is up and running. If not, open the links in the browser.
    private boolean isConnectedToCustomTabService;

    private Subscriber<PhotosResultDM> nasaMarsPhotoSubscriber;

    public ExplorerTabHostPresenterLayer(RoverExplorerTabHostActivity activity) {
        this.activity = activity;
        prepareCustomTabs();
    }

    @Override
    public void checkInternetConnectivity() {
        if(!UtilityMethods.isNetworkAvailable())
            activity.showToast(activity.getResources()
                    .getString(R.string.no_internet), Toast.LENGTH_LONG);
    }

    @Override
    public void getValuesFromIntent() {
        roverName = activity.getIntent()
                .getStringExtra(RoverExplorerConstants.roverNameExtra);

        roverSol = activity.getIntent()
                .getStringExtra(RoverExplorerConstants.roverMaxSolExtra);
    }

    @Override
    public void handleOptionsSelected(MenuItem item) {
        switch (item.getItemId()) {

            case android.R.id.home:
                // Respond to the action bar's Up/Home button.
                activity.supportFinishAfterTransition();
                break;

            case R.id.action_rover_details:
                openInformationPage();
                break;
        }
    }

    @Override
    public void setViewsValue() {

        // Set the toolbar title
        activity.setToolbarTitle(roverName);

        // Set the rover image
        switch (roverName) {

            case GeneralConstants.Curiosity:
                activity.setCollapsibleToolbarImage(R.drawable.curiosity);
                break;

            case GeneralConstants.Opportunity:
                activity.setCollapsibleToolbarImage(R.drawable.spirit);
                break;

            case GeneralConstants.Spirit:
                activity.setCollapsibleToolbarImage(R.drawable.opportunity);
                break;
        }
    }

    @Override
    public void prepareAndImplementViewPager(final ViewPager viewPager, final TabLayout tabLayout) {

        final int numberOfInitialTabs = 10;
        final int numberOfTabsLeftAfterWhichToAdd = 2;
        final int numberOfTabsToAdd = 5;
        final int offScreenPageLimit = 1;

        final List<Fragment> fragmentList = new ArrayList<>();
        final List<String> solList = new ArrayList<>();
        final TabData tabData = new TabData();

        if(roverSol == null || roverSol.isEmpty()) {
            getMaxSol(roverName);
            this.viewPager = viewPager;
            this.tabLayout = tabLayout;
        } else {
            roverSolTracker = Integer.valueOf(roverSol);

            // Initiate and add fragments for the new SOL tabs respectively.
            for(int fragmentCount = roverSolTracker;
                fragmentCount > Integer.valueOf(roverSol) - numberOfInitialTabs;
                fragmentCount--) {

                // Arguments to be sent to the fragment
                Bundle args = new Bundle();
                args.putInt(RoverExplorerConstants.roverSolTrackExtra, roverSolTracker);
                args.putString(RoverExplorerConstants.roverNameExtra, roverName);

                fragmentList.add(Fragment.instantiate(activity,
                        RoverExplorerFragment.class.getName(), args));
                solList.add(String.valueOf(roverSolTracker));
                tabData.setFragmentList(fragmentList);
                tabData.setSolList(solList);

                roverSolTracker--;
            }

            final ViewPagerAdapter viewPagerAdapter =
                    new ViewPagerAdapter(activity.getSupportFragmentManager(), tabData);
            viewPager.setAdapter(viewPagerAdapter);
            tabLayout.setupWithViewPager(viewPager);
            viewPager.setOffscreenPageLimit(offScreenPageLimit);
            viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
                @Override
                public void onPageScrolled(int position, float positionOffset,
                                           int positionOffsetPixels) {

                }

                @Override
                public void onPageSelected(int position) {

                    // Check if the user has reached the second last or last tab.
                    // If he/ she has and the SOL is not below 1, add required number of tabs
                    if(fragmentList.size() - position <= numberOfTabsLeftAfterWhichToAdd
                            && roverSolTracker > 0) {

                        for(int newTabCount = 0; newTabCount <= numberOfTabsToAdd; newTabCount++) {

                            Bundle args = new Bundle();
                            args.putInt(RoverExplorerConstants.roverSolTrackExtra, roverSolTracker);
                            args.putString(RoverExplorerConstants.roverNameExtra, roverName);
                            fragmentList.add(Fragment.instantiate(activity,
                                    RoverExplorerFragment.class.getName(), args));
                            solList.add(String.valueOf(roverSolTracker));
                            tabData.setFragmentList(fragmentList);
                            tabData.setSolList(solList);

                            roverSolTracker--;
                        }
                        viewPagerAdapter.notifyDataSetChanged();
                    }
                }

                @Override
                public void onPageScrollStateChanged(int state) {

                }
            });
        }
    }

    /**
     * Method to get the max SOL of the selected rover. This method is called only when the
     * previous activity fails to pass maxSol to this activity.
     * This request is sent only if the previous fails to send maxSol.
     * @param roverName
     */
    private void getMaxSol(final String roverName) {

        // Define the observer
        Observable<PhotosResultDM> nasaMarsPhotosObservable
                = MarsExplorerApplication.getApplicationInstance()
                .getNasaMarsPhotosApiInterface()
                .getPhotosBySol(true, true, roverName, "1");

        // Define the subscriber
        nasaMarsPhotoSubscriber = new Subscriber<PhotosResultDM>() {
            @Override
            public void onCompleted() {
                Timber.i("Max SOL of %s found", roverName);
            }

            @Override
            public void onError(Throwable ex) {
                ex.printStackTrace();
            }

            @Override
            public void onNext(PhotosResultDM photosResultDM) {
                //TODO: Handle no data condition

                roverSol = photosResultDM.photos().get(0).rover().maxSol().toString();
                prepareAndImplementViewPager(viewPager, tabLayout);
            }
        };

        // Subscribe to the observable
        nasaMarsPhotosObservable
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.newThread())
                .subscribe(nasaMarsPhotoSubscriber);
    }

    @Override
    public void unsubscribeMaxSolRequest() {
        if(nasaMarsPhotoSubscriber != null)
            nasaMarsPhotoSubscriber.unsubscribe();
    }

    /**
     * Method to prepare the CustomTabs.
     */
    private void prepareCustomTabs() {
        final String CUSTOM_TAB_PACKAGE_NAME = "com.android.chrome";

        CustomTabsServiceConnection customTabsServiceConnection =
                new CustomTabsServiceConnection() {
                    @Override
                    public void onCustomTabsServiceConnected(ComponentName name,
                                                             CustomTabsClient client) {
                        customTabsClient = client;
                        customTabsClient.warmup(0L);
                        customTabsSession = customTabsClient.newSession(null);
                        switch (roverName) {
                            case GeneralConstants.Curiosity:
                                customTabsSession.mayLaunchUrl(Uri.parse(CURIOSITY_WIKIPEDIA_PAGE),
                                        null, null);
                                break;
                            case GeneralConstants.Opportunity:
                                customTabsSession.mayLaunchUrl(Uri.parse(OPPORTUNITY_WIKIPEDIA_PAGE),
                                        null, null);
                                break;
                            case GeneralConstants.Spirit:
                                customTabsSession.mayLaunchUrl(Uri.parse(SPIRIT_WIKIPEDIA_PAGE),
                                        null, null);
                                break;
                        }
                    }

                    @Override
                    public void onServiceDisconnected(ComponentName name) {
                        customTabsClient = null;
                    }
                };

        isConnectedToCustomTabService = CustomTabsClient.bindCustomTabsService(activity,
                CUSTOM_TAB_PACKAGE_NAME, customTabsServiceConnection);

        customTabsIntent = new CustomTabsIntent.Builder(customTabsSession)
                .setToolbarColor(ContextCompat.getColor(activity, R.color.colorPrimary))
                .setShowTitle(true)
                .setStartAnimations(activity, R.anim.slide_up_enter, R.anim.stay)
                .setExitAnimations(activity, R.anim.stay, R.anim.slide_down_exit)
                .build();

        customTabsIntent.intent.putExtra(UtilityMethods.customTabReferrerKey(),
                UtilityMethods.customTabReferrerString());
    }

    /**
     * Method to open information page for respective rovers.
     */
    private void openInformationPage() {

        switch (roverName) {
            case GeneralConstants.Curiosity:
                if(isConnectedToCustomTabService)
                    customTabsIntent.launchUrl(activity, Uri.parse(CURIOSITY_WIKIPEDIA_PAGE));
                else {
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW,
                            Uri.parse(CURIOSITY_WIKIPEDIA_PAGE));
                    activity.startActivity(browserIntent);
                }
                break;
            case GeneralConstants.Opportunity:
                if(isConnectedToCustomTabService)
                    customTabsIntent.launchUrl(activity, Uri.parse(OPPORTUNITY_WIKIPEDIA_PAGE));
                else {
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW,
                            Uri.parse(OPPORTUNITY_WIKIPEDIA_PAGE));
                    activity.startActivity(browserIntent);
                }
                break;
            case GeneralConstants.Spirit:
                if(isConnectedToCustomTabService)
                    customTabsIntent.launchUrl(activity, Uri.parse(SPIRIT_WIKIPEDIA_PAGE));
                else {
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW,
                            Uri.parse(SPIRIT_WIKIPEDIA_PAGE));
                    activity.startActivity(browserIntent);
                }
                break;
        }
    }
}
