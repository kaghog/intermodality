package simulation;


import org.matsim.core.config.CommandLine;

/**
 * @author kaghog created on 16.04.2021
 * @project swiss_intermodality
 */

public class SimulationParameter {

    private double intermodalSearchFactor;
    private double minAccessDist;
    private String personFilterType;

    public void setBikeParams(CommandLine cmd) {

    }

    public void setIntermodalParams(CommandLine cmd) {

        //check for intermodal search distance factor
        if (cmd.hasOption("intermodal-search-factor")) {
            setIntermodalSearchFactor(Double.parseDouble(cmd.getOption("intermodal-search-factor").get()));
        } else {
            //setting default to 60% is random, an appropriate value should be tested as overlaps can occur and could
            //cause pure walk trips (DRT-DRT with no pt legs)
            setIntermodalSearchFactor(0.6);
        }

        //check for minimum stop access/egress distance setting
        //This is used to find stops in relation to the search distance factor (SDF), so for 500m min-dist
        //the farthest distance stops will be searched for drt must be greater than 500/SDF
        if (cmd.hasOption("min-access-dist")) {
            setMinAccessDist(Double.parseDouble(cmd.getOption("min-access-dist").get()));
        } else {
            setMinAccessDist(500.0); //setting default to 500
        }

        if (cmd.hasOption("person-filter-type")) {
            setPersonFilterType(cmd.getOption("person-filter-type").get());
        } else {
            setPersonFilterType("none");
        }
    }


    public void setMinAccessDist (double minAccessDist) {
        if (minAccessDist >= 0) { //ToDo also check that it is less than 60% of initial search radius
            this.minAccessDist = minAccessDist;
        } else {
            throw new IllegalStateException("Invalid minimum DRT Access Distance  defined: " + minAccessDist);
        }
    }
    public double getMinAccessDist() {return minAccessDist; }

    //set intermodal search factor
    public void setIntermodalSearchFactor (double intermodalSearchFactor) {
        if (intermodalSearchFactor > 0 | intermodalSearchFactor <= 1) {
            this.intermodalSearchFactor = intermodalSearchFactor;
        } else {
            throw new IllegalStateException("Invalid intermodal search distance factor: " + intermodalSearchFactor + " .The search distance factor should between 0 and 1");
        }
    }
    public double getIntermodalSearchFactor() { return intermodalSearchFactor; }

    public void setPersonFilterType (String personFilterType){
        if (personFilterType.equals("bikeOwnership") | personFilterType.equals("bikeOwnershipPTSubscription") | personFilterType.equals("none") ){
            this.personFilterType = personFilterType;
        } else {
            throw new IllegalStateException("Invalid person-filter-type: " + personFilterType + ". Available options are: bikeOwnership & bikeOwnershipPTSubscription");
        }
    }

    public String getPersonFilterType() {return personFilterType;}

}

