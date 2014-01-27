package team071;


import battlecode.common.*;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Random;
/**
 * Created by dominicfrost on 1/12/14.
 */
public class RobotUtil {
	static int DefenseChannelOffset = 10000;
	static int DefenseGoalLocation = 64999;
	static int[] OffenseGoalLocations = {2,3,4,5};
	static int OffenseGoalDestroyed = 7;
	static int OffenseCurrentGoalOffset = 8;
	static int buildingProgress = 9;
	static int numDefendingGoal = 10;
	static int startGroup = 11;
	static int startGroupGO = 12;
	static int sendToAttack = 13;
	static int towerLocation = 14;
	static int[] groupAttackLocation = {15,16,17,18,19};
	static int[] numberInjuredInGroup = {22,23,24,25,26}; 
	static int groupUpdate = 20;
	static int groupLeaderPicked = 21;
    static Direction allDirections[] = Direction.values();
    static Random rand = new Random();
    
    public static boolean micro(RobotController rc, int groupNum) throws GameActionException{
    	Robot[] robotsNearArr = rc.senseNearbyGameObjects(Robot.class, 35);
    	boolean result = false;
		ArrayList<Robot> enemiesNear = new ArrayList<Robot>();
		ArrayList<Robot> teammatesNear = new ArrayList<Robot>();

		for(int i = 0; i < robotsNearArr.length; i++){
			if(robotsNearArr[i].getTeam() != rc.getTeam() && rc.senseRobotInfo(robotsNearArr[i]).type != RobotType.HQ){
				enemiesNear.add(robotsNearArr[i]);
			}else if(robotsNearArr[i].getTeam() == rc.getTeam() && rc.senseRobotInfo(robotsNearArr[i]).type != RobotType.PASTR && rc.senseRobotInfo(robotsNearArr[i]).type != RobotType.NOISETOWER){
				teammatesNear.add(robotsNearArr[i]);
			}
		}
		
		//if the the robot is injured flee
		if(rc.readBroadcast(rc.getRobot().getID() + 50) == 1){
			if(rc.getHealth() > 70){
				rc.broadcast(rc.getRobot().getID() + 50, 0);
				rc.broadcast(numberInjuredInGroup[groupNum], rc.readBroadcast(numberInjuredInGroup[groupNum]) - 1);
			}else{
				if(enemiesNear.size() > 0){//run like a little girl
					int counterx = 0;
					int countery = 0;
					for(Robot opp: enemiesNear){
						MapLocation oppLoc = rc.senseLocationOf(opp);
						counterx += oppLoc.x;
						countery += oppLoc.y;
					}
					MapLocation avg = new MapLocation(counterx / enemiesNear.size(), countery / enemiesNear.size());
		
					Direction dirToGoal = rc.getLocation().directionTo(avg).opposite();
					RobotUtil.moveInDirection(rc, dirToGoal, "sneak");
					result = true;
				}
			}
		}else{//if were not injured and we have decent health then attack!
			if(rc.getHealth() > 40){
				if(rc.readBroadcast(groupAttackLocation[groupNum]) != -1){//if its group has a target
					MapLocation groupAttackSpot = intToMapLoc(rc.readBroadcast(groupAttackLocation[groupNum]));
					//if you can sense the spot, and theres still a robot attack, else tell everyone its gone and try and attack any other bots around
					if(rc.canSenseSquare(groupAttackSpot)){
						GameObject objAtLoc = rc.senseObjectAtLocation(groupAttackSpot);
						if(objAtLoc != null && objAtLoc.getTeam() != rc.getTeam()){
							if(rc.canAttackSquare(groupAttackSpot)){
								rc.attackSquare(groupAttackSpot);
								result = true;
							}else{
								moveInDirection(rc, rc.getLocation().directionTo(groupAttackSpot), "sneak");
								result = true;
							}
						}else{
							System.out.println("Group " + groupNum + " destroyed robot at " + groupAttackSpot);
							rc.broadcast(groupAttackLocation[groupNum], -1);
						}
					}else{
						moveInDirection(rc, rc.getLocation().directionTo(groupAttackSpot), "sneak");
						result = true;
					}
				}else if(enemiesNear.size() > 0){
					//if someone has posted a group attack loc
					for(int i = 0; i < enemiesNear.size(); i++){
						Robot r = enemiesNear.get(i);
						MapLocation attackSpot = rc.senseLocationOf(r);
						if(rc.canAttackSquare(attackSpot)){
							rc.attackSquare(attackSpot);
							rc.broadcast(groupAttackLocation[groupNum], mapLocToInt(attackSpot));
							System.out.println("Group " + groupNum + " is attacking " + attackSpot);
							result = true;
						}
					}
				}
			}else{//RRRRRUUUUUUUUUUUUUNNNNNNNNNNNNNNNN
				rc.broadcast(rc.getRobot().getID() + 50, 1);//IM HURT I need to hide
				rc.broadcast(numberInjuredInGroup[groupNum], rc.readBroadcast(numberInjuredInGroup[groupNum]) + 1);
					//compute opposite direction of direction towards average enemy position
					if(enemiesNear.size() > 0){
					int counterx = 0;
					int countery = 0;
					for(Robot opp: enemiesNear){
						MapLocation oppLoc = rc.senseLocationOf(opp);
						counterx += oppLoc.x;
						countery += oppLoc.y;
					}
					MapLocation avg = new MapLocation(counterx / enemiesNear.size(), countery / enemiesNear.size());
					Direction dirToGoal = rc.getLocation().directionTo(avg).opposite();
					RobotUtil.moveInDirection(rc, dirToGoal, "sneak");
					result = true;
				}
				
			}
		}

		return result;
    }
    
    
    public static void logMap(double[][] map) {
        int mapWidth = map.length;
        int mapHeight = map[0].length;
        System.out.print("\n");
        for (int i = 0; i < mapHeight; i++) {
            for (int j = 0; j < mapWidth; j++) {
                System.out.print((int)map[j][i] + " ");
            }
            System.out.print("\n");
        }
    }

    public static void logMap(int[][] map) {
        int mapWidth = map.length;
        int mapHeight = map[0].length;
        System.out.print("\n");
        for (int i = 0; i < mapHeight; i++) {
            for (int j = 0; j < mapWidth; j++) {
                System.out.print(map[j][i] + " ");
            }
            System.out.print("\n");
        }
    }

    public static int mapLocToInt(MapLocation m){
        return (m.x*100 + m.y);
    }

    public static MapLocation intToMapLoc(int i){
        return new MapLocation(i/100,i%100);
    }

    public static void intelligentSpawn(RobotController rc, Direction goalDirection) throws GameActionException{
        int[] directionPriority = {0, 1, -1, 2, -2, 3, -3, 4};
        if(rc.isActive() && rc.senseRobotCount() < GameConstants.MAX_ROBOTS){
            for(int direction:directionPriority){
                int trialDir = (goalDirection.ordinal() + direction + 8) % 8;
                if(rc.canMove(allDirections[trialDir])){
                    rc.spawn(allDirections[trialDir]);
                    rc.broadcast(startGroup, rc.readBroadcast(startGroup) + 1);
                    break;
                }
            }
        }
    }

    public static void moveInDirection(RobotController rc, Direction goalDirection, String type) throws GameActionException {
        int[] directionPriority = {0, 1, -1, 2, -2, 3, -3, 4};
        if(rc.isActive()){
            for(int direction:directionPriority){
                int trialDir = (goalDirection.ordinal() + direction + 8) % 8;
                if(rc.canMove(allDirections[trialDir])){
                    if(type.equals("sneak")) {
                        rc.sneak(allDirections[trialDir]);
                    } else {
                        rc.move(allDirections[trialDir]);
                    }
                    break;
                }
            }
        }
    }
    public static int[][] assessMap(RobotController rc, int[][] map) {
        int mapWidth = rc.getMapWidth();
        int ogMapWidth = mapWidth;
        int mapHeight = rc.getMapHeight();
        for (int i = 0; i < mapHeight + 2; i++) {
            for (int j = 0; j < mapWidth + 2; j++) {
                int tile;
                if(i == 0 || j == 0 || i == ogMapWidth + 1 || j == mapHeight + 1) { // pad the map to easily find corners
                    tile = 2;
                    map[i][j] = tile;
                    map[ogMapWidth + 1 - i][mapHeight + 1 - j] = tile;
                } else {
                    tile = rc.senseTerrainTile(new MapLocation(i - 1, j - 1)).ordinal();
                    map[i][j] = tile;
                    map[ogMapWidth + 1 - i][mapHeight + 1 - j] = tile;
                }
                map[i][j] = tile > 2 ? 2 : tile;
                map[ogMapWidth + 1 - i][mapHeight + 1 - j] = tile > 2 ? 2 : tile;
            }
            mapWidth--;
        }
        return map;
    }

    public static int[][] assessMapWithDirection(RobotController rc, MapLocation goal, int[][] map) throws GameActionException {
        ArrayDeque<MapLocation> queue = new ArrayDeque<MapLocation>();
        int mapWidth = map.length;
        int mapHeight = map[0].length;
        int currentX;
        int currentY;
        MapLocation currentLocation;
        map[goal.x][goal.y] = 9;
        MapLocation temp = rc.senseHQLocation();
        map[temp.x][temp.y] = 9;
        temp = rc.senseEnemyHQLocation();
        map[temp.x][temp.y] = 9;
        
        // we want map locations in the queue
        queue.add(goal);
        // and the distance values / direction in the graph __|_
        while(!queue.isEmpty()) {
            intelligentSpawn(rc, rc.getLocation().directionTo(goal));
            currentLocation = queue.poll();
            currentX = currentLocation.x;
            currentY = currentLocation.y;
            
         // check the northern square
            if(currentY != 0 && map[currentX][currentY-1] == 0 && rc.senseTerrainTile(new MapLocation(currentX, currentY-1)).ordinal() != 2) {
                map[currentX][currentY-1] = 5;
                queue.add(new MapLocation(currentX, currentY-1));
                
            }
            // check the north eastern square
            if(currentY != 0 && currentX != mapWidth-1 && map[currentX+1][currentY-1] == 0 && rc.senseTerrainTile(new MapLocation(currentX+1, currentY-1)).ordinal() != 2) {
                map[currentX+1][currentY-1] = 6;
                queue.add(new MapLocation(currentX+1, currentY-1));
                
            }
            // check the eastern square
            if(currentX != mapWidth-1 && map[currentX+1][currentY] == 0 && rc.senseTerrainTile(new MapLocation(currentX+1, currentY)).ordinal() != 2) {
                map[currentX+1][currentY] = 7;
                queue.add(new MapLocation(currentX+1, currentY));
            }
            // check the south eastern square
            if(currentX != mapWidth-1 && currentY != mapHeight-1 && map[currentX+1][currentY+1] == 0 && rc.senseTerrainTile(new MapLocation(currentX+1, currentY+1)).ordinal() != 2) {
                map[currentX+1][currentY+1] = 8;
                queue.add(new MapLocation(currentX+1, currentY+1));
            }
            // check the southern square
            if(currentY != mapHeight-1 && map[currentX][currentY+1] == 0 && rc.senseTerrainTile(new MapLocation(currentX, currentY+1)).ordinal() != 2) {
                map[currentX][currentY+1] = 1;
                queue.add(new MapLocation(currentX, currentY+1));
            }
            // check the south western square
            if(currentX != 0 && currentY != mapHeight-1 && map[currentX-1][currentY+1] == 0 && rc.senseTerrainTile(new MapLocation(currentX-1, currentY+1)).ordinal() != 2) {
                map[currentX-1][currentY+1] = 2;
                queue.add(new MapLocation(currentX-1, currentY+1));
            }
            // check the western square
            if(currentX != 0 && map[currentX-1][currentY] == 0 && rc.senseTerrainTile(new MapLocation(currentX-1, currentY)).ordinal() != 2) {
                map[currentX-1][currentY] = 3;
                queue.add(new MapLocation(currentX-1, currentY));
            }
            // check the north western square
            if(currentX != 0 && currentY != 0 && map[currentX-1][currentY-1] == 0 && rc.senseTerrainTile(new MapLocation(currentX-1, currentY-1)).ordinal() != 2) {
                map[currentX-1][currentY-1] = 4;
                queue.add(new MapLocation(currentX-1, currentY-1));
            }	
             
            
        }
        return map;
    }

    public static ArrayList<Direction> bugPath(MapLocation start, MapLocation destination, int[][] map) throws GameActionException {
        
        ArrayList<Direction> path = new ArrayList<Direction>();
        MapLocation currentLocation = start;
        MapLocation targetLocation;
        int currentDistance = currentLocation.distanceSquaredTo(destination);
        int shortestDistance = currentDistance;
        System.out.println("Start loc: (" + start.x + ", " + start.y + ")");
        System.out.println("Destination loc: (" + destination.x + ", " + destination.y + ")");
        int roundNum = Clock.getRoundNum();;
        while(true) {
            if (Clock.getRoundNum() - roundNum > 100) {return path;}
            // we are at out destination
            if(currentLocation.x == destination.x && currentLocation.y == destination.y) {
                break;
            }
            Direction dir = currentLocation.directionTo(destination);
            targetLocation = currentLocation.add(dir);
//            System.out.println("VAL: " + map[targetLocation.x][targetLocation.y] + "TargetLoc: " + targetLocation);
            // if we can move in the direction of the destination
            if(map[targetLocation.x+1][targetLocation.y+1] != 2 && map[targetLocation.x+1][targetLocation.y+1] != 3) {
                path.add(dir);
//                System.out.println("1: " + currentLocation + " -> " + dir + " -> " + targetLocation);
                currentLocation = targetLocation;
                currentDistance = currentLocation.distanceSquaredTo(destination);
                shortestDistance = currentDistance;

            // we can not move towards the destination because there is a wall
            } else {
                // location to the destination should not be a wall
                MapLocation temp = currentLocation.add(currentLocation.directionTo(destination));
                // we are not closer and the direction towards the destination is a wall
                while(currentDistance >= shortestDistance || (map[temp.x+1][temp.y+1] == 2 || map[temp.x+1][temp.y+1] == 3)) {
                    if (Clock.getRoundNum() - roundNum > 100) {return path;}
                    // while the targetLocation is a wall, rotate and update to new target location
                    // this deals with if we need to rotate left
                    int flag = 0;
                    while (map[targetLocation.x+1][targetLocation.y+1] == 2 || map[targetLocation.x+1][targetLocation.y+1] == 3) {
                        if (Clock.getRoundNum() - roundNum > 100) {return path;}
                        flag += 1;
                        dir = dir.rotateLeft();
//                        System.out.println("Rotate Left, DIR is now: " + dir);
                        targetLocation = currentLocation.add(dir);
                    }
                    // if the wall on our right disappears
                    // this deals with if we need to rotate right
                    MapLocation wallLocation = currentLocation.add(dir.rotateRight().rotateRight());
                    if (flag == 1) {
                        path.add(dir);
                        currentLocation = targetLocation;
                        currentDistance = currentLocation.distanceSquaredTo(destination);
                        break;
                    }
                    if(map[wallLocation.x+1][wallLocation.y+1] != 2 && map[wallLocation.x+1][wallLocation.y+1] != 3) {
                        dir = dir.rotateRight().rotateRight();
//                        System.out.println("Rotate Right x2, DIR is now: " + dir);
                        targetLocation = currentLocation.add(dir);
                    } else {
                        targetLocation = currentLocation.add(dir);
                    }
                    path.add(dir);
//                    System.out.println("2: " + currentLocation + " -> " + dir + " -> " + targetLocation);
                    currentLocation = targetLocation;
                    targetLocation = currentLocation.add(dir);
                    currentDistance = currentLocation.distanceSquaredTo(destination);
                    temp = currentLocation.add(currentLocation.directionTo(destination));
                }
            }
        }
        return path;
    }
    
    public static MapLocation sensePASTRGoal(RobotController rc){
    	double[][] growthMap = rc.senseCowGrowth();
    	int mapSize = growthMap.length;
    	double[] colGrowth = new double[mapSize];
    	double[] rowGrowth = new double[mapSize];
    	
    	for(int i = 0; i < mapSize; i++){
    		double colVal = 0.0;
    		double rowVal = 0.0;
    		for(int j = 0; j < mapSize; j++){
    			colVal += growthMap[i][j];
    			rowVal += growthMap[j][i];
    			//System.out.print(growthMap[j][i] + " ");
    		}
    		//System.out.println();
    		colGrowth[i] = colVal;
    		rowGrowth[i] = rowVal;
    	}
    	
    	double maxGrowth = 0.0;
    	int growthXLoc = 0;
    	int growthYLoc = 0;
    	for(int i = 0; i < rowGrowth.length; i++){
    		for(int j = 0; j < colGrowth.length; j++){
    			if((colGrowth[i] + rowGrowth[j]) > maxGrowth && rc.senseTerrainTile(new MapLocation(i,j)).ordinal() != 2){
    				maxGrowth = rowGrowth[i] + colGrowth[j];
    				//System.out.println(maxGrowth);
    				growthXLoc = i;
    				growthYLoc = j;	
    			}
    		}
    	}
    	return new MapLocation(growthXLoc,growthYLoc);
    }
    
    public static MapLocation sensePASTRGoal2(RobotController rc){
    	double[][] growthMap = rc.senseCowGrowth();
    	double counter = 0.0;
    	double counterx = 0.0;
    	double countery = 0.0;
    	
    	for(int i = 0; i < growthMap.length; i++){
    		for(int j = 0; j < growthMap[0].length - i; j++){
    			double growth = growthMap[i][j];
    			if(growth > 0){
    				counter+=growth;
    				counterx += (i*growth);
    				countery += (j*growth);
    			}
    		}
    	}
    	if((int)(counterx/counter) == 0 && (int)(countery/counter) == 0 ){
    		return new MapLocation(1,1);
    	}else return new MapLocation((int)(counterx/counter), (int)(countery/counter));
    }
    
    public static MapLocation sensePASTRGoal3(RobotController rc, int mapWidth, int mapHeight){

    	int avg = mapWidth + mapHeight;
    	double[][] map = rc.senseCowGrowth();
    	mapWidth--;
    	mapHeight--;
    	double largestCowGrowth = -1;
    	MapLocation maxLoc = new MapLocation(-1,-1);
        int closestGoalDistance = 999;
        double totalCows;
    	
    	for(int i = 0; i <  avg; i++){
    		int currentWidth = rand.nextInt() % mapWidth;
    		int currentHeight = rand.nextInt() % mapHeight;
    		MapLocation potentialGoal = new MapLocation(currentWidth, currentHeight);
    		
    		if(rc.senseTerrainTile(potentialGoal).ordinal() != 2 && rc.senseTerrainTile(potentialGoal).ordinal() != 3 && potentialGoal.distanceSquaredTo(rc.senseEnemyHQLocation()) > 100){
                totalCows = 0;
	    		for(int j = -4; j <= 4; j++){
	    			for(int k = -4; k <= 4; k++){
	    				int x = currentWidth+j;
	    				int y = currentHeight+k;
                        // make sure that we are only accessing squares within the bounds of our map array
	    				if(x >= 0 && y >= 0 && x < mapWidth && y < mapHeight){
                            totalCows += map[x][y];
	    				}
	    			}
	    		}
	    		if(totalCows > largestCowGrowth) {
                    largestCowGrowth = totalCows;
                    closestGoalDistance = potentialGoal.distanceSquaredTo(rc.senseHQLocation());
                    System.out.println("1. last best goal: " + maxLoc + ", new best goal: " + potentialGoal + ", total growth: " + totalCows);
                    maxLoc = potentialGoal;
	    		}

                // if the new sample has the same amount of cows as the previous best location
                // then chose the location that is closest to our HQ
                if (totalCows == largestCowGrowth) {
                    if (closestGoalDistance > potentialGoal.distanceSquaredTo(rc.senseHQLocation())) {
                        closestGoalDistance = potentialGoal.distanceSquaredTo(rc.senseHQLocation());
                        System.out.println("2. last best goal: " + maxLoc + ", new best goal: " + potentialGoal + ", total growth: " + totalCows);
                        maxLoc = potentialGoal;
                    }
                }
    		}
    	}
    	
    	return maxLoc;
    }
    
    public static void broadcastMap(RobotController rc, int[][] map, int offset) throws GameActionException{
    	int mapWidth = map.length;
    	int mapHeight = map[0].length;
    	for(int i = 0; i < mapWidth; i++){
    		for(int j = 0; j < mapHeight; j++){
    			MapLocation ml = new MapLocation(i,j);
    			rc.broadcast(mapLocToInt(ml) + offset, map[i][j]);
    		}
    	}
    }
    
    public static int[][] readMapFromBroadcast(RobotController rc, int offset) throws GameActionException{
    	int mapWidth = rc.getMapWidth();
    	int mapHeight = rc.getMapHeight();
    	int[][] map = new int[mapWidth][mapHeight];
    	for(int i = 0; i < mapWidth; i++){
    		for(int j = 0; j < mapHeight; j++){
    			map[i][j] = rc.readBroadcast(offset + mapLocToInt(new MapLocation(i,j)));
    		}
    	}
    	return map;
    }
    public static MapLocation getPastrToMakeGoal(RobotController rc, int[] channels, MapLocation ourPASTR) throws GameActionException{
    	MapLocation[] pastrLocs = rc.sensePastrLocations(rc.getTeam().opponent());

        MapLocation closestPASTR = null;
        int smallestDistance = 9999;
        boolean newPASTRLocation;
        // loop through all enemy PASTRS - we need to find the one closest to our PASTR
    	for(MapLocation pastr: pastrLocs){
    		newPASTRLocation = true;
            // loop through all channels
    		for(int channel: channels){
                // don't calculate a whole map if one already exists for it.
    			if(rc.readBroadcast(channel) == mapLocToInt(pastr)){
                    newPASTRLocation = false;
    			}
    		}
    		if(newPASTRLocation){
    			if(pastr.distanceSquaredTo(ourPASTR) < smallestDistance) {
                    closestPASTR = pastr;
                    smallestDistance = pastr.distanceSquaredTo(ourPASTR);
                }
    		}
    	}
    	return closestPASTR;
    }
    
    public static int getNewGoalPastr(RobotController rc, int lastOffset, int[] channels) throws GameActionException{
    	ArrayList<Integer> populatedChannels = new ArrayList<Integer>();
    	for(int channel : channels){
    		if(channel != lastOffset && rc.readBroadcast(channel) != 0){
    			populatedChannels.add(channel);
    		}
    	}
    	if(populatedChannels.size() > 0){
    		int max = Integer.MAX_VALUE;
        	int result = -1;
        	for(int channel: populatedChannels){
        		int current = RobotUtil.intToMapLoc(rc.readBroadcast(DefenseGoalLocation)).distanceSquaredTo(RobotUtil.intToMapLoc(rc.readBroadcast(channel)));
        		if(current < max){
        			max = current;
        			result = channel;
        		}
        	}
        	return result;
    	} else {
            return -1;
        }
    }
    
    public static void movingBugPath(RobotController rc, MapLocation goal) throws GameActionException{
    	MapLocation currentLocation = rc.getLocation();
    	if(currentLocation.equals(goal)){
    		
    	}else{
    		if(rc.readBroadcast(1) == 0){
    			Direction dir = currentLocation.directionTo(goal);
    			if(rc.canMove(dir)){
    				moveInDirection(rc, dir, "move");
    			}else{
    				while(true){
    					if((rc.senseTerrainTile(currentLocation.add(allDirections[dir.ordinal() + 2])).ordinal() == 2 || rc.senseTerrainTile(currentLocation.add(allDirections[dir.ordinal() + 2])).ordinal() == 3) && (rc.senseTerrainTile(currentLocation.add(dir)).ordinal() != 2 && rc.senseTerrainTile(currentLocation.add(dir)).ordinal() == 3)){
    						moveInDirection(rc, dir, "move");
    						//rc.broadcast(ToBeat)
    						break;
    					}else dir = dir.rotateLeft();
    					
    				}
    			}
    		}else{
    			
    		}
    	}
    }
}
