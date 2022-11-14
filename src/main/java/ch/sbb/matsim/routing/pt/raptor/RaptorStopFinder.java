package ch.sbb.matsim.routing.pt.raptor;

import java.util.List;

import org.matsim.api.core.v01.population.Person;
import org.matsim.facilities.Facility;

/**
 * Find potential stops for access or egress, based on a start or end coordinate.
 *
 * @author mrieser / Simunto GmbH
 */
@FunctionalInterface
public interface RaptorStopFinder {

	public enum Direction { ACCESS, EGRESS }

	//@balacm added fromFacility and toFacility of the whole pt trip to be able to compute the total trip length and implement the search factor constraint
	List<InitialStop> findStops(Facility fromFacility, Facility toFacility, Person person, double departureTime, RaptorParameters parameters, SwissRailRaptorData data, Direction type);

}
