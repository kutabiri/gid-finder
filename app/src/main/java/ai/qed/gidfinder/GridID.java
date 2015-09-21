package ai.qed.gidfinder;

import android.location.Location;

import org.osgeo.proj4j.CRSFactory;
import org.osgeo.proj4j.CoordinateReferenceSystem;
import org.osgeo.proj4j.CoordinateTransform;
import org.osgeo.proj4j.CoordinateTransformFactory;
import org.osgeo.proj4j.ProjCoordinate;

/**
 * Note that the cells are identified like this: 2W, 1W, 0W, 0E, 1E, 2E.
 */
public class GridID {
    private boolean eastingNegative;
    private int easting;
    private boolean northingNegative;
    private int northing;
    private int index;

    private static final String standardProjectionString =
            "+title=long/lat:WGS84 +proj=longlat +ellps=WGS84 +datum=WGS84 +units=degrees";
    private static final String lambertProjectionString =
            "+proj=laea +ellps=WGS84 +lon_0=20 +lat_0=5 +units=m +no_defs";

    public GridID(boolean eastingNegative, int easting, boolean northingNegative, int northing, int index) {
        this.eastingNegative = eastingNegative;
        this.easting = easting;
        this.northingNegative = northingNegative;
        this.northing = northing;
        this.index = index;
    }

    private static int intfloor(double x) {
        return (int) (Math.floor(x) + 0.5);
    }

    /* Takes real numbers in [0, 1000) */
    private static int calculateCellID(double x, double y) {
        return intfloor(x / 100) * 10 + intfloor(y / 100);
    }

    private static double floorMod(double a, double b) {
        return ((a % b) + b) % b;
    }

    static public GridID fromLocation(Location location) {
        Location lambert = convertToLambert(location);
        int easting = intfloor(Math.abs(lambert.getLongitude()) / 1000);
        int northing = intfloor(Math.abs(lambert.getLatitude()) / 1000);

        boolean eastingNegative = lambert.getLongitude() < 0;
        boolean northingNegative = lambert.getLatitude() < 0;

        int cellID = calculateCellID(
                floorMod(lambert.getLongitude(), 1000), floorMod(lambert.getLatitude(), 1000));

        return new GridID(eastingNegative, easting, northingNegative, northing, cellID); 
    }

    private static Location convertToLambert(Location location) {
        CoordinateTransformFactory ctFactory = new CoordinateTransformFactory();
        CRSFactory csFactory = new CRSFactory();
        CoordinateReferenceSystem WGS84 = csFactory.createFromParameters("WGS84", standardProjectionString);
        CoordinateReferenceSystem lambert = csFactory.createFromParameters("lambert", lambertProjectionString);
        CoordinateTransform trans = ctFactory.createTransform(WGS84, lambert);
        ProjCoordinate p = new ProjCoordinate();
        ProjCoordinate p2 = new ProjCoordinate();
        p.x = location.getLongitude();
        p.y = location.getLatitude();
        trans.transform(p, p2);
        Location res = new Location("");
        res.setLongitude(p2.x);
        res.setLatitude(p2.y);
        return res;
    }

    private String formatCoordinate(boolean isNegative, int coordinate, String positive, String negative) {
        String prefix = isNegative? negative : positive;
        int number = coordinate;

        return String.format("%s%d", prefix, number);
    }

    @Override
    public String toString() {
        String eastingString = formatCoordinate(eastingNegative, easting, "E", "W");
        String northingString = formatCoordinate(northingNegative, northing, "N", "S");
        return String.format("%s-%s-%d\n", eastingString, northingString, index);
    }

    public int getSubcell() {
        return index;
    }
 }
