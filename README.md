Grid ID (GID) Finder
=====================
Purpose: Determine GID in real-time from current GPS location.

We use the Lambert Azimuthal Equal Area (LAEA) projection system to cover the world with a global grid, where each cell is 1km-by-1km.

We set 20°E, 5°N as our origin O, which lies near the center of Africa.

The Grid ID (GID) for a cell is then calculated by taking the LAEA coordinates (x,y) in meters (relative to origin O) of any point strictly within the interior, and then computing E-value = ⌊abs(x)/1000⌋ and N-value=⌊abs(y)/1000⌋. The GID string is then written as:

    GID=E43-N51

where 43 and 51 are example E-values and N-values, respectively. Lastly, if x is negative, then we replace "E" with "W". Similarly, if y is negative, then we replace "N" with "S".

Subcells: Each 1km-by-1km cell can also be further divided into 100m-by-100m subcells, indexed from 0 to 99, ordered in raster fashion such that the far-west column increases from 0 to 9 moving north, and the next column over increases from 10 to 19 moving north, and so on until we reach the east-most column which increases from 90 to 99 moving north. 

An example GID for a subcell can then be denoted as follows: 
    
    GID=E1108-S877-62


Testing within emulator
------------------------

    $ telnet localhost 5554
    > geo fix -121.45356 46.51119 4392


QED | http://qed.ai
