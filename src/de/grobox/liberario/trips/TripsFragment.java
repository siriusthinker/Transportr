package de.grobox.liberario.trips;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import com.orangegangsters.github.swipyrefreshlayout.library.SwipyRefreshLayout;
import com.orangegangsters.github.swipyrefreshlayout.library.SwipyRefreshLayout.OnRefreshListener;
import com.orangegangsters.github.swipyrefreshlayout.library.SwipyRefreshLayoutDirection;

import java.util.ArrayList;
import java.util.Date;

import javax.inject.Inject;

import de.grobox.liberario.R;
import de.grobox.liberario.fragments.TransportrFragment;
import de.grobox.liberario.networks.TransportNetworkManager;
import de.grobox.liberario.ui.LceAnimator;
import de.schildbach.pte.dto.Location;
import de.schildbach.pte.dto.QueryTripsResult;

import static com.orangegangsters.github.swipyrefreshlayout.library.SwipyRefreshLayoutDirection.BOTH;
import static com.orangegangsters.github.swipyrefreshlayout.library.SwipyRefreshLayoutDirection.BOTTOM;
import static de.grobox.liberario.utils.Constants.DATE;
import static de.grobox.liberario.utils.Constants.FROM;
import static de.grobox.liberario.utils.Constants.IS_DEPARTURE;
import static de.grobox.liberario.utils.Constants.LOADER_TRIPS;
import static de.grobox.liberario.utils.Constants.TO;
import static de.grobox.liberario.utils.Constants.VIA;
import static de.grobox.liberario.utils.TransportrUtils.getDragDistance;
import static de.schildbach.pte.dto.QueryTripsResult.Status.OK;

public class TripsFragment extends TransportrFragment implements LoaderCallbacks<QueryTripsResult>, OnRefreshListener {

	final static String TAG = TripsFragment.class.getName();

	@Inject
	TransportNetworkManager manager;

	private ProgressBar progressBar;
	private SwipyRefreshLayout swipe;
	private RecyclerView list;
	private TripAdapter adapter;

	private Location from, to;
	@Nullable
	private Location via;
	private Date date;
	private boolean departure;

	public static TripsFragment newInstance(Location from, @Nullable Location via, Location to, Date date, boolean departure) {
		TripsFragment f = new TripsFragment();
		Bundle args = new Bundle();
		args.putSerializable(FROM, from);
		args.putSerializable(VIA, via);
		args.putSerializable(TO, to);
		args.putSerializable(DATE, date);
		args.putBoolean(IS_DEPARTURE, departure);
		f.setArguments(args);
		return f;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.fragment_trips, container, false);
		getComponent().inject(this);

		// initialize arguments
		Bundle bundle = getArguments();
		from = (Location) bundle.getSerializable(FROM);
		via = (Location) bundle.getSerializable(VIA);
		to = (Location) bundle.getSerializable(TO);
		date = (Date) bundle.getSerializable(DATE);
		departure = bundle.getBoolean(IS_DEPARTURE);
		if (from == null || to == null || date == null) throw new IllegalArgumentException();

		// Progress Bar
		progressBar = (ProgressBar) v.findViewById(R.id.progressBar);

		// Swipe to Refresh
		swipe = (SwipyRefreshLayout) v.findViewById(R.id.swipe);
		swipe.setColorSchemeResources(R.color.accent);
		swipe.setDistanceToTriggerSync(getDragDistance(getContext()));
		swipe.setOnRefreshListener(this);

		list = (RecyclerView) v.findViewById(R.id.list);
		list.setLayoutManager(new LinearLayoutManager(getContext()));
		boolean showLineName = manager.getTransportNetwork() != null && manager.getTransportNetwork().hasGoodLineNames();
		adapter = new TripAdapter(new ArrayList<ListTrip>(), null, getContext(), showLineName);
		adapter.setHasStableIds(false);
		list.setAdapter(adapter);

		LceAnimator.showLoading(progressBar, list, null);

		getLoaderManager().initLoader(LOADER_TRIPS, null, this).forceLoad();

		return v;
	}

	public void setSwipeEnabled(boolean enabled) {
		if (swipe != null) swipe.setDirection(enabled ? BOTH : BOTTOM);
	}

	@Override
	public void onRefresh(SwipyRefreshLayoutDirection direction) {
		// TODO
		swipe.setRefreshing(false);
	}

	@Override
	public Loader<QueryTripsResult> onCreateLoader(int id, Bundle args) {
		return new TripsLoader(getContext(), manager, from, via, to, date, departure);
	}

	@Override
	public void onLoadFinished(Loader<QueryTripsResult> loader, @Nullable QueryTripsResult result) {
		if (result != null && result.status == OK) {
			adapter.addAll(ListTrip.getList(result.trips));
			LceAnimator.showContent(progressBar, list, null);
		} else {
			// TODO show error message
			LceAnimator.showContent(progressBar, list, null);
		}
	}

	@Override
	public void onLoaderReset(Loader<QueryTripsResult> loader) {
		adapter.clear();
	}

}