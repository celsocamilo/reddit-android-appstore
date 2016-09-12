package subreddit.android.appstore.backend.scrapers.caching;


import android.content.Context;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.functions.Function;
import io.realm.Realm;
import io.realm.RealmConfiguration;
import subreddit.android.appstore.AppStoreApp;
import subreddit.android.appstore.backend.data.AppInfo;
import subreddit.android.appstore.backend.scrapers.ScrapeResult;
import timber.log.Timber;

public class ScrapeDiskCache {
    static final String TAG = AppStoreApp.LOGPREFIX + "ScrapeDiskCache";

    public ScrapeDiskCache(Context context) {
        RealmConfiguration realmConfig = new RealmConfiguration.Builder(context, context.getCacheDir()).build();
        Realm.setDefaultConfiguration(realmConfig);
    }

    public void put(AppInfo appInfo, ScrapeResult scrapeResult) {
        Timber.tag(TAG).d("Storing <%s,%s>", appInfo, scrapeResult);
        Realm realm = Realm.getDefaultInstance();
        realm.beginTransaction();
        realm.copyToRealmOrUpdate(new CachedScrape(appInfo, scrapeResult));
        realm.commitTransaction();
    }

    public Observable<ScrapeResult> get(final AppInfo appInfo) {
        return Observable.create(
                new ObservableOnSubscribe<CachedScrape>() {
                    @Override
                    public void subscribe(ObservableEmitter<CachedScrape> e) throws Exception {
                        Realm realm = Realm.getDefaultInstance();
                        CachedScrape cachedScrape = realm.where(CachedScrape.class).equalTo("id", appInfo.hashCode()).findFirst();
                        if (cachedScrape != null) e.onNext(cachedScrape);
                        e.onComplete();
                    }
                })
                .map(new Function<CachedScrape, ScrapeResult>() {
                    @Override
                    public ScrapeResult apply(CachedScrape cachedScrape) throws Exception {
                        ScrapeResult scrapeResult = cachedScrape.toBaseScrapeResult();
                        Timber.tag(TAG).d("Returning get(%s): %s", appInfo, scrapeResult);
                        return scrapeResult;
                    }
                });
    }
}
