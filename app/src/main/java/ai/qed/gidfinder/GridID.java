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
    private int gidX;
    private int gidY;
    private int index;
    private int xOffset;
    private int yOffset;
    private double latitude;
    private double longitude;

    private static final String standardProjectionString =
            "+title=long/lat:WGS84 +proj=longlat +ellps=WGS84 +datum=WGS84 +units=degrees";
    private static final String lambertProjectionString =
            "+proj=laea +ellps=WGS84 +lon_0=20 +lat_0=5 +units=m +no_defs";

    public GridID(double latitude, double longitude, int gidX, int gidY, int index, double x, double y) {
        this.gidX = gidX;
        this.gidY = gidY;
        this.index = index;
        this.xOffset = intfloor(x % 100);
        this.yOffset = intfloor(y % 100);
        this.latitude = Math.round(latitude * 1000000.00) / 1000000.00;
        this.longitude = Math.round(longitude * 1000000.00) / 1000000.00;
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
        int gidX = intfloor(lambert.getLongitude() / 1000);
        int gidY = intfloor(lambert.getLatitude() / 1000);

        double x = floorMod(lambert.getLongitude(), 1000);
        double y = floorMod(lambert.getLatitude(), 1000);

        int cellID = calculateCellID(x, y);

        return new GridID(location.getLatitude(), location.getLongitude(), gidX, gidY, cellID, x, y);
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

    public String toGIDString() {
        return String.format("GID\n%s, %s, %d", gidX, gidY, index);
    }

    public String getLatString() {
        return Double.toString(latitude);
    }

    public String getLongString() {
        return Double.toString(longitude);
    }

    public int getSubcell() {
        return index;
    }

    public int getXOffset() {
        return xOffset;
    }

    public int getYOffset() {
        return yOffset;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

 }
