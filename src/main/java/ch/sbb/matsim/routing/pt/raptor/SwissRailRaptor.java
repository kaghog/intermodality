
package ch.sbb.matsim.routing.pt.raptor;

import ch.sbb.matsim.config.SwissRailRaptorConfigGroup;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.facilities.Facility;
import org.matsim.pt.router.TransitRouter;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Provides public transport route search capabilities using an implementation of the
 * RAPTOR algorithm underneath.
 *
 * @author mrieser / SBB
 * updated by @balacm
 */
public class SwissRailRaptor implements TransitRouter {

    private static final Logger log = Logger.getLogger(SwissRailRaptor.class);

    private final SwissRailRaptorData data;
    private final SwissRailRaptorCore raptor;
    private final RaptorParametersForPerson parametersForPerson;
    private final RaptorRouteSelector defaultRouteSelector;
    private final RaptorStopFinder stopFinder;

    private boolean treeWarningShown = false;

    public SwissRailRaptor( final SwissRailRaptorData data, RaptorParametersForPerson parametersForPerson,
                            RaptorRouteSelector routeSelector,
                            RaptorStopFinder stopFinder,
                            RaptorInVehicleCostCalculator inVehicleCostCalculator,
                            RaptorTransferCostCalculator transferCostCalculator) {
        this.data = data;
        this.raptor = new SwissRailRaptorCore(data, inVehicleCostCalculator, transferCostCalculator);
        this.parametersForPerson = parametersForPerson;
        this.defaultRouteSelector = routeSelector;
        this.stopFinder = stopFinder;
    }

    @Override
    public List<Leg> calcRoute(Facility fromFacility, Facility toFacility, double departureTime, Person person) {
        RaptorParameters parameters = this.parametersForPerson.getRaptorParameters(person);
        if (parameters.getConfig().isUseRangeQuery()) {
            return this.performRangeQuery(fromFacility, toFacility, departureTime, person, parameters);
        } else {
            List<InitialStop> accessStops = findAccessStops(fromFacility, toFacility, person, departureTime, parameters);
            List<InitialStop> egressStops = findEgressStops(fromFacility, toFacility, person, departureTime, parameters);

            RaptorRoute foundRoute = this.raptor.calcLeastCostRoute(departureTime, fromFacility, toFacility, accessStops, egressStops, parameters, person);
            RaptorRoute directWalk = createDirectWalk(fromFacility, toFacility, departureTime, person, parameters);

            if (foundRoute != null && foundRoute.parts.size() != 0 && !this.hasNoPtLeg(foundRoute.parts)) {
                if (directWalk.getTotalCosts() * parameters.getDirectWalkFactor() < foundRoute.getTotalCosts()) {
                    foundRoute = directWalk;
                }

                List<Leg> legs = RaptorUtils.convertRouteToLegs(foundRoute, this.data.config.getTransferWalkMargin());
                return legs;
            } else {
                Logger var10000;
                if (person == null) {
                    var10000 = log;
                    double var10001 = fromFacility.getCoord().getX();
                    var10000.debug("No route found for person null: trip from x=" + var10001 + ",y=" + fromFacility.getCoord().getY() + " departure at " + departureTime + " to x=" + toFacility.getCoord().getX() + ",y=" + toFacility.getCoord().getY());
                } else {
                    var10000 = log;
                    Id var12 = person.getId();
                    var10000.debug("No route found for person " + var12 + ": trip from x=" + fromFacility.getCoord().getX() + ",y=" + fromFacility.getCoord().getY() + " departure at " + departureTime + " to x=" + toFacility.getCoord().getX() + ",y=" + toFacility.getCoord().getY());
                }

                return null;
            }
        }
    }

    private boolean hasNoPtLeg(List<RaptorRoute.RoutePart> parts) {
        Iterator var2 = parts.iterator();

        RaptorRoute.RoutePart part;
        do {
            if (!var2.hasNext()) {
                return true;
            }

            part = (RaptorRoute.RoutePart)var2.next();
        } while(part.line == null);

        return false;
    }

    private List<Leg> performRangeQuery(Facility fromFacility, Facility toFacility, double desiredDepartureTime, Person person, RaptorParameters parameters) {
        SwissRailRaptorConfigGroup srrConfig = parameters.getConfig();

//        Object attr = this.personAttributes.getAttribute(person.getId().toString(), this.subpopulationAttribute);
//	    Object attr = person.getAttributes().getAttribute( this.subpopulationAttribute ) ;
//        String subpopulation = attr == null ? null : attr.toString();
        String subpopulation = PopulationUtils.getSubpopulation( person );
        SwissRailRaptorConfigGroup.RangeQuerySettingsParameterSet rangeSettings = srrConfig.getRangeQuerySettings(subpopulation);

        double earliestDepartureTime = desiredDepartureTime - (double) rangeSettings.getMaxEarlierDeparture();
        double latestDepartureTime = desiredDepartureTime + (double) rangeSettings.getMaxLaterDeparture();

        if (this.defaultRouteSelector instanceof ConfigurableRaptorRouteSelector) {
            ConfigurableRaptorRouteSelector selector = (ConfigurableRaptorRouteSelector) this.defaultRouteSelector;

            SwissRailRaptorConfigGroup.RouteSelectorParameterSet params = srrConfig.getRouteSelector(subpopulation);

            selector.setBetaTransfer(params.getBetaTransfers());
            selector.setBetaTravelTime(params.getBetaTravelTime());
            selector.setBetaDepartureTime(params.getBetaDepartureTime());
        }

        return this.calcRoute(fromFacility, toFacility, earliestDepartureTime, desiredDepartureTime, latestDepartureTime, person, this.defaultRouteSelector);
    }

    public List<Leg> calcRoute(Facility fromFacility, Facility toFacility, double earliestDepartureTime, double desiredDepartureTime, double latestDepartureTime, Person person) {
        return calcRoute(fromFacility, toFacility, earliestDepartureTime, desiredDepartureTime, latestDepartureTime, person, this.defaultRouteSelector);
    }

    public List<Leg> calcRoute(Facility fromFacility, Facility toFacility, double earliestDepartureTime, double desiredDepartureTime, double latestDepartureTime, Person person, RaptorRouteSelector selector) {
        RaptorParameters parameters = this.parametersForPerson.getRaptorParameters(person);
        List<InitialStop> accessStops = findAccessStops(fromFacility, toFacility, person, desiredDepartureTime, parameters);
        List<InitialStop> egressStops = findEgressStops(fromFacility, toFacility, person, desiredDepartureTime, parameters);

        List<RaptorRoute> foundRoutes = this.raptor.calcRoutes(earliestDepartureTime, desiredDepartureTime, latestDepartureTime, fromFacility, toFacility, accessStops, egressStops, parameters, person);
        RaptorRoute foundRoute = selector.selectOne(foundRoutes, desiredDepartureTime);
        RaptorRoute directWalk = createDirectWalk(fromFacility, toFacility, desiredDepartureTime, person, parameters);

        if (foundRoute != null && foundRoute.parts.size() != 0 && !this.hasNoPtLeg(foundRoute.parts)) {
            if (directWalk.getTotalCosts() * parameters.getDirectWalkFactor() < foundRoute.getTotalCosts()) {
                foundRoute = directWalk;
            }

            List<Leg> legs = RaptorUtils.convertRouteToLegs(foundRoute, this.data.config.getTransferWalkMargin());
            return legs;
        } else {
            Logger var10000;
            if (person == null) {
                var10000 = log;
                double var10001 = fromFacility.getCoord().getX();
                var10000.debug("No route found for person null: trip from x=" + var10001 + ",y=" + fromFacility.getCoord().getY() + " departure at " + desiredDepartureTime + " to x=" + toFacility.getCoord().getX() + ",y=" + toFacility.getCoord().getY());
            } else {
                var10000 = log;
                Id var18 = person.getId();
                var10000.debug("No route found for person " + var18 + ": trip from x=" + fromFacility.getCoord().getX() + ",y=" + fromFacility.getCoord().getY() + " departure at " + desiredDepartureTime + " to x=" + toFacility.getCoord().getX() + ",y=" + toFacility.getCoord().getY());
            }

            return null;
        }
    }

    public List<RaptorRoute> calcRoutes(Facility fromFacility, Facility toFacility, double earliestDepartureTime, double desiredDepartureTime, double latestDepartureTime, Person person) {
        RaptorParameters parameters = this.parametersForPerson.getRaptorParameters(person);
        List<InitialStop> accessStops = findAccessStops(fromFacility, toFacility, person, desiredDepartureTime, parameters);
        List<InitialStop> egressStops = findEgressStops(fromFacility, toFacility, person, desiredDepartureTime, parameters);

        List<RaptorRoute> foundRoutes = this.raptor.calcRoutes(earliestDepartureTime, desiredDepartureTime, latestDepartureTime, fromFacility, toFacility, accessStops, egressStops, parameters, person);
        RaptorRoute directWalk = createDirectWalk(fromFacility, toFacility, desiredDepartureTime, person, parameters);

        if (foundRoutes == null) {
            foundRoutes = new ArrayList<>(1);
        }
        Iterator<RaptorRoute> iter = ((List)foundRoutes).iterator();

        while(true) {
            RaptorRoute foundRoute;
            do {
                if (!iter.hasNext()) {
                    if (((List)foundRoutes).isEmpty() || directWalk.getTotalCosts() * parameters.getDirectWalkFactor() < ((RaptorRoute)((List)foundRoutes).get(0)).getTotalCosts()) {
                        ((List)foundRoutes).add(directWalk);
                    }

                    return (List)foundRoutes;
                }

                foundRoute = (RaptorRoute)iter.next();
            } while(foundRoute.parts.size() != 0 && !this.hasNoPtLeg(foundRoute.parts));

            iter.remove();
        }
    }

    public Map<Id<TransitStopFacility>, SwissRailRaptorCore.TravelInfo> calcTree(TransitStopFacility fromStop, double departureTime, RaptorParameters parameters, Person person) {
        return this.calcTree((Collection)Collections.singletonList(fromStop), departureTime, parameters, person);
    }

    public Map<Id<TransitStopFacility>, SwissRailRaptorCore.TravelInfo> calcTree(Collection<TransitStopFacility> fromStops, double departureTime, RaptorParameters parameters, Person person) {
        if (this.data.config.getOptimization() != RaptorStaticConfig.RaptorOptimization.OneToAllRouting && !this.treeWarningShown) {
            log.warn("SwissRailRaptorData was not initialized with full support for tree calculations and may result in unexpected results. Use `RaptorStaticConfig.setOptimization(RaptorOptimization.OneToAllRouting)` to fix this issue.");
            this.treeWarningShown = true;
        }

        List<InitialStop> accessStops = new ArrayList();
        Iterator var7 = fromStops.iterator();

        while(var7.hasNext()) {
            TransitStopFacility stop = (TransitStopFacility)var7.next();
            accessStops.add(new InitialStop(stop, 0.0, 0.0, 0.0, (String)null));
        }

        return this.calcLeastCostTree(accessStops, departureTime, parameters, person);
    }

    public Map<Id<TransitStopFacility>, SwissRailRaptorCore.TravelInfo> calcTree(Facility fromFacility, double departureTime, Person person) {
        RaptorParameters parameters = this.parametersForPerson.getRaptorParameters(person);
        List<InitialStop> accessStops = findAccessStops(fromFacility, fromFacility, person, departureTime, parameters);
        return this.calcLeastCostTree(accessStops, departureTime, parameters, person);
    }

    private Map<Id<TransitStopFacility>, SwissRailRaptorCore.TravelInfo> calcLeastCostTree(Collection<InitialStop> accessStops, double departureTime, RaptorParameters parameters, Person person) {
        return this.raptor.calcLeastCostTree(departureTime, accessStops, parameters, person);
    }

    public SwissRailRaptorData getUnderlyingData() {
        return this.data;
    }
//@balacm updates accessEgress stops to have from and to facility
    private List<InitialStop> findAccessStops(Facility fromFacility, Facility toFacility, Person person, double departureTime, RaptorParameters parameters) {
        return this.stopFinder.findStops(fromFacility, toFacility, person, departureTime, parameters, this.data, RaptorStopFinder.Direction.ACCESS);
    }

    private List<InitialStop> findEgressStops(Facility fromFacility, Facility toFacility, Person person, double departureTime, RaptorParameters parameters) {
        return this.stopFinder.findStops(fromFacility, toFacility, person, departureTime, parameters, this.data, RaptorStopFinder.Direction.EGRESS);
    }

    // TODO: replace with call to FallbackRoutingModule ?!
    private RaptorRoute createDirectWalk(Facility fromFacility, Facility toFacility, double departureTime, Person person, RaptorParameters parameters) {
        double beelineDistance = CoordUtils.calcEuclideanDistance(fromFacility.getCoord(), toFacility.getCoord());
        double walkTime = beelineDistance / parameters.getBeelineWalkSpeed();
        double walkCost_per_s = -parameters.getMarginalUtilityOfTravelTime_utl_s(TransportMode.walk);
        double walkCost = walkTime * walkCost_per_s;
        double beelineDistanceFactor = this.data.config.getBeelineWalkDistanceFactor();

        RaptorRoute route = new RaptorRoute(fromFacility, toFacility, walkCost);
        route.addNonPt((TransitStopFacility)null, (TransitStopFacility)null, departureTime, walkTime, beelineDistance * beelineDistanceFactor, TransportMode.walk);
        return route;
    }

    public static class Builder {
        private final SwissRailRaptorData data;
        private RaptorParametersForPerson parametersForPerson;
        private RaptorRouteSelector routeSelector = new LeastCostRaptorRouteSelector();
        private RaptorStopFinder stopFinder = new DefaultRaptorStopFinder(new DefaultRaptorIntermodalAccessEgress(), (Map)null);
        private RaptorInVehicleCostCalculator inVehicleCostCalculator = new DefaultRaptorInVehicleCostCalculator();
        private RaptorTransferCostCalculator transferCostCalculator = new DefaultRaptorTransferCostCalculator();

        public Builder(SwissRailRaptorData data, Config config) {
            this.data = data;
            this.parametersForPerson = new DefaultRaptorParametersForPerson(config);
        }

        public Builder with(RaptorParametersForPerson parametersForPerson) {
            this.parametersForPerson = parametersForPerson;
            return this;
        }

        public Builder with(RaptorRouteSelector routeSelector) {
            this.routeSelector = routeSelector;
            return this;
        }

        public Builder with(RaptorStopFinder stopFinder) {
            this.stopFinder = stopFinder;
            return this;
        }

        public Builder with(RaptorInVehicleCostCalculator inVehicleCostCalculator) {
            this.inVehicleCostCalculator = inVehicleCostCalculator;
            return this;
        }

        public Builder with(RaptorTransferCostCalculator transferCostCalculator) {
            this.transferCostCalculator = transferCostCalculator;
            return this;
        }

        public SwissRailRaptor build() {
            return new SwissRailRaptor(this.data, this.parametersForPerson, this.routeSelector, this.stopFinder, this.inVehicleCostCalculator, this.transferCostCalculator);
        }
    }

}