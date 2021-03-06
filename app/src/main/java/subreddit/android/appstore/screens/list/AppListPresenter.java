package subreddit.android.appstore.screens.list;

import android.os.Bundle;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import subreddit.android.appstore.backend.data.AppInfo;
import subreddit.android.appstore.backend.reddit.wiki.WikiRepository;
import subreddit.android.appstore.screens.navigation.CategoryFilter;
import timber.log.Timber;


public class AppListPresenter implements AppListContract.Presenter {
    final WikiRepository repository;
    final CategoryFilter categoryFilter;
    private Disposable listUpdater;
    private Disposable tagUpdater;
    AppListContract.View view;

    public AppListPresenter(WikiRepository repository, CategoryFilter categoryFilter) {
        this.repository = repository;
        this.categoryFilter = categoryFilter;
    }

    @Override
    public void onCreate(Bundle bundle) {

    }

    @Override
    public void onAttachView(final AppListContract.View view) {
        this.view = view;
        view.showLoadingScreen();
        Observable<List<AppInfo>> filteredData = repository.getAppList()
                .observeOn(Schedulers.computation())
                .map(appInfos -> {
                    ArrayList<AppInfo> data = new ArrayList<>(appInfos);
                    List<AppInfo> filteredData1 = new ArrayList<>();
                    for (AppInfo app : data) {
                        if ((app.getPrimaryCategory().equals(categoryFilter.getPrimaryCategory()) || categoryFilter.getPrimaryCategory() == null) &&
                                (app.getSecondaryCategory().equals(categoryFilter.getSecondaryCategory()) || categoryFilter.getSecondaryCategory() == null)) {
                            filteredData1.add(app);
                        }
                    }
                    Collections.sort(filteredData1);
                    return filteredData1;
                }).replay().refCount();

        listUpdater = filteredData
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(appInfos -> {
                    Timber.d("showAppList(%s items)", appInfos.size());
                    AppListPresenter.this.view.showAppList(appInfos);
                });

        tagUpdater = filteredData
                .observeOn(Schedulers.computation())
                .map(new Function<Collection<AppInfo>, TagMap>() {
                    @Override
                    public TagMap apply(Collection<AppInfo> appInfos) throws Exception {
                        return new TagMap(appInfos);
                    }
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(tagMap -> {
                    Timber.d("updateTagCount(%s)", tagMap);
                    AppListPresenter.this.view.updateTagCount(tagMap);
                });

        filteredData
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(appInfos -> {
                    if (appInfos.size()<1) {
                        view.showError();
                    }
                });
    }

    @Override
    public void onDetachView() {
        listUpdater.dispose();
        tagUpdater.dispose();
        view = null;
    }

    @Override
    public void onSaveInstanceState(Bundle bundle) {

    }

    @Override
    public void onDestroy() {

    }


    @Override
    public void refreshData() {
        view.showLoadingScreen();
        repository.refresh();
    }
}
