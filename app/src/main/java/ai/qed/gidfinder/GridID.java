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

    public static GridID fromLocation(Location location) {
        Location lambert = convertToLambert(location.getLatitude(), location.getLongitude());
        int gidX = intfloor(lambert.getLongitude() / 1000);
        int gidY = intfloor(lambert.getLatitude() / 1000);

        double x = floorMod(lambert.getLongitude(), 1000);
        double y = floorMod(lambert.getLatitude(), 1000);

        int cellID = calculateCellID(x, y);

        return new GridID(location.getLatitude(), location.getLongitude(), gidX, gidY, cellID, x, y);
    }

    public static GridID fromLatLong(double lat, double lon) {
        Location lambert = convertToLambert(lat, lon);
        int gidX = intfloor(lambert.getLongitude() / 1000);
        int gidY = intfloor(lambert.getLatitude() / 1000);

        double x = floorMod(lambert.getLongitude(), 1000);
        double y = floorMod(lambert.getLatitude(), 1000);

        int cellID = calculateCellID(x, y);

        return new GridID(lat, lon, gidX, gidY, cellID, x, y);
    }

    public static Location fromGID(String gid) {
        int gidX = 0;
        int gidY = 0;
        String[] parts = gid.split("-");

        if (parts.length != 3) {
            throw new IllegalArgumentException(gid + " not valid");
        }

        if (parts[0].startsWith("W")) {
            gidX = Integer.parseInt(parts[0].substring(1));
        }
        else if (parts[0].startsWith("E")) {
            gidX = -Integer.parseInt(parts[0].substring(1));
        }
        else {
            throw new IllegalArgumentException(gid + " not valid");
        }

        if (parts[1].startsWith("N")) {
            gidY = Integer.parseInt(parts[1].substring(1));
        }
        else if (parts[1].startsWith("S")) {
            gidY = -Integer.parseInt(parts[1].substring(1));
        }
        else {
            throw new IllegalArgumentException(gid + " not valid");
        }

        int x = Integer.parseInt(parts[2]) / 100 / 10;
        int y = Integer.parseInt(parts[2]) % 100;

        CoordinateTransformFactory ctFactory = new CoordinateTransformFactory();
        CRSFactory csFactory = new CRSFactory();
        CoordinateReferenceSystem WGS84 = csFactory.createFromParameters("WGS84", standardProjectionString);
        CoordinateReferenceSystem lambert = csFactory.createFromParameters("lambert", lambertProjectionString);
        CoordinateTransform trans = ctFactory.createTransform(lambert, WGS84);
        ProjCoordinate p = new ProjCoordinate();
        ProjCoordinate p2 = new ProjCoordinate();
        p.x = gidX * 1000 + x;
        p.y = gidY * 1000 + y;
        trans.transform(p, p2);
        Location res = new Location("");
        res.setLongitude(p2.x);
        res.setLatitude(p2.y);

        return res;
    }

    private static Location convertToLambert(double lat, double lon) {
        CoordinateTransformFactory ctFactory = new CoordinateTransformFactory();
        CRSFactory csFactory = new CRSFactory();
        CoordinateReferenceSystem WGS84 = csFactory.createFromParameters("WGS84", standardProjectionString);
        CoordinateReferenceSystem lambert = csFactory.createFromParameters("lambert", lambertProjectionString);
        CoordinateTransform trans = ctFactory.createTransform(WGS84, lambert);
        ProjCoordinate p = new ProjCoordinate();
        ProjCoordinate p2 = new ProjCoordinate();
        p.x = lon;
        p.y = lat;
        trans.transform(p, p2);
        Location res = new Location("");
        res.setLongitude(p2.x);
        res.setLatitude(p2.y);
        return res;
    }

    public String toGIDHeaderString() {
        return String.format("GID\n%s%s-%s%s-%d", gidX>=0 ? 'W' : 'E', Math.abs(gidX), gidY>=0 ? 'N' : 'S', Math.abs(gidY), index);
    }

    public String toGIDString() {
        return String.format("%s%s-%s%s-%d", gidX>=0 ? 'W' : 'E', Math.abs(gidX), gidY>=0 ? 'N' : 'S', Math.abs(gidY), index);
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
