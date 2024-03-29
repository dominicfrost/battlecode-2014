package Broseph;

import battlecode.common.*;

import java.util.ArrayList;
import java.util.Random;

public class RobotPlayer {
	static Random rand;
	public static enum missions{
        defense, offense;
    }
	
	static int DefenseChannelOffset = 10000;
	static int DefenseGoalLocation = 1;
	static int[] OffenseChannelOffsets = {20000,30000,40000,50000};
	static int[] OffenseGoalLocations = {2,3,4,5};
	static int OffenseNewLocation = 6;
	static int OffenseGoalDestroyed = 7;
	static int OffenseCurrentGoalOffset = 8;
	static int buildingProgress = 9;
	
	public static void run(RobotController rc) {
		rand = new Random();
		Direction[] directions = Direction.values();
		int mapWidth = rc.getMapWidth();
		int mapHeight = rc.getMapHeight();
		int[][] map = new int[mapWidth][mapHeight];
		MapLocation goal = new MapLocation(0,0);
		boolean first = true;
		MapLocation currentLocation = null;
		missions robotMission = missions.defense;
		boolean wasOffense = false;
		
		
        while(true) {
        	
			if (rc.getType() == RobotType.HQ) {
				try {
					Robot[] enemiesNear = rc.senseNearbyGameObjects(Robot.class, 35, rc.getTeam().opponent());
					for(Robot r: enemiesNear){
						if(rc.isActive() && rc.canAttackSquare(rc.senseLocationOf(r)) && rc.senseRobotInfo(r).type != RobotType.HQ){
							rc.attackSquare(rc.senseLocationOf(r));
							break;
						}
					}
					
					//Check if a robot is spawnable and spawn one if it is
					if (rc.isActive() ) {
						if(rc.senseRobotCount() < 25){
							RobotUtil.intelligentSpawn(rc, rc.getLocation().directionTo(rc.senseEnemyHQLocation()));
						}
						if(rc.readBroadcast(DefenseGoalLocation) == 0){
							currentLocation = rc.getLocation();
							//sense a goal location based on pastr growth
							while(true){
								goal = RobotUtil.sensePASTRGoal3(rc, mapWidth, mapHeight);
								if(!goal.equals(new MapLocation(-1,-1))){
									break;
								}
							}
							//Pathing Algorithm
	                        map = RobotUtil.assessMapWithDirection(rc, goal, map);
	                        RobotUtil.logMap(map);
	                        //broadcast the map out for other robots to read
	                        RobotUtil.broadcastMap(rc, map, DefenseChannelOffset);
	                        //let everyone know the goal location
	                        rc.broadcast(DefenseGoalLocation, RobotUtil.mapLocToInt(goal));
						}else{
							
							rc.broadcast(OffenseNewLocation, 0);
							if(rc.readBroadcast(OffenseGoalDestroyed) == 1){
								System.out.println(RobotUtil.intToMapLoc(rc.readBroadcast(rc.readBroadcast(OffenseCurrentGoalOffset)))+" from "+rc.readBroadcast(OffenseCurrentGoalOffset) + " has been eliminated");
								rc.broadcast(rc.readBroadcast(OffenseCurrentGoalOffset), 0);
							}
							
							//if a new map can be computed do so
							for(int channel: OffenseGoalLocations){
								if(rc.readBroadcast(channel) == 0){
									goal = RobotUtil.getPastrToMakeGoal(rc, OffenseGoalLocations);
									if(goal != null){
										map = RobotUtil.assessMapWithDirection(rc, goal, new int[mapWidth][mapHeight]);
										rc.broadcast(channel, RobotUtil.mapLocToInt(goal));
										RobotUtil.broadcastMap(rc, map, channel*10000);
										if(rc.readBroadcast(OffenseCurrentGoalOffset) == 0){
											rc.broadcast(OffenseCurrentGoalOffset, channel);
										}
										System.out.println(goal+ " has been added to " +channel);
										break;
									}
								}
							}
							//if the goal is destroyed update the goal
							if(rc.readBroadcast(OffenseGoalDestroyed) == 1){
								int oldOffset = rc.readBroadcast(OffenseCurrentGoalOffset);
								int n = RobotUtil.getNewGoalPastr(rc, oldOffset, OffenseGoalLocations);
								if(n != -1){
									rc.broadcast(OffenseCurrentGoalOffset, n);
									rc.broadcast(OffenseGoalDestroyed, 0);
									rc.broadcast(OffenseNewLocation, 1);
								}
							}
						}
					}
				} catch (Exception e) {
					System.out.println("HQ Exception");
				}
			} else if (rc.getType() == RobotType.SOLDIER) {
				try {
					if (rc.isActive()) {
						
						Robot[] enemiesNearArr = rc.senseNearbyGameObjects(Robot.class, 35, rc.getTeam().opponent());
						ArrayList<Robot> enemiesNear = new ArrayList<Robot>();
						for(int i = 0; i < enemiesNearArr.length; i++){
							enemiesNear.add(enemiesNearArr[i]);
						}
						
						for(int i = 0; i < enemiesNear.size(); i++){
							Robot r = enemiesNear.get(i);
							if(rc.senseRobotInfo(r).type == RobotType.HQ){
								enemiesNear.remove(i);
								i--;
							}else if(rc.canAttackSquare(rc.senseLocationOf(r))){
								rc.attackSquare(rc.senseLocationOf(r));
								rc.yield();
							}
						}
						
						if(first){
							//get a map if possible
							if(rc.readBroadcast(DefenseGoalLocation) > 0){
								if(rc.readBroadcast(OffenseCurrentGoalOffset) == 0){
									goal = RobotUtil.intToMapLoc(rc.readBroadcast(DefenseGoalLocation));
									map = RobotUtil.readMapFromBroadcast(rc, DefenseChannelOffset);
									robotMission = missions.defense;
								}else{
									goal = RobotUtil.intToMapLoc(rc.readBroadcast(rc.readBroadcast(OffenseCurrentGoalOffset)));
									map = RobotUtil.readMapFromBroadcast(rc, rc.readBroadcast(OffenseCurrentGoalOffset) * 10000);
									robotMission = missions.offense;
								}
								first = false;
							}
						}else{
							currentLocation = rc.getLocation();
							
							if(robotMission == missions.defense){
								if(wasOffense && rc.readBroadcast(OffenseNewLocation) == 1){
									map = RobotUtil.readMapFromBroadcast(rc, rc.readBroadcast(OffenseCurrentGoalOffset) * 10000);
									goal = RobotUtil.intToMapLoc(rc.readBroadcast(rc.readBroadcast(OffenseCurrentGoalOffset)));
									robotMission = missions.offense;
									wasOffense = false;
									System.out.println("New goal recieved at "+goal);
								}
								//if a pastr and noisetower havent been made/assigned to a bot
								if(rc.readBroadcast(buildingProgress) < 2){
									if(currentLocation.equals(goal)){
										rc.construct(RobotType.PASTR);
										rc.broadcast(buildingProgress, 1);
									}else if(rc.readBroadcast(buildingProgress) == 1 && currentLocation.distanceSquaredTo(goal) < 4){
										rc.construct(RobotType.NOISETOWER);
										rc.broadcast(buildingProgress, 2);
									}else{
										int intToGoal = map[currentLocation.x][currentLocation.y] - 1;
										Direction dirToGoal = directions[intToGoal];
										RobotUtil.moveInDirection(rc, dirToGoal, "move");
									}
								}
								//move towards goal
								int intToGoal = map[currentLocation.x][currentLocation.y] - 1;
								Direction dirToGoal = directions[intToGoal];
								RobotUtil.moveInDirection(rc, dirToGoal, "sneak");
								
							}else{
								//if there is a new goal pastr update path map
								if(rc.readBroadcast(OffenseNewLocation) == 1){
									map = RobotUtil.readMapFromBroadcast(rc, rc.readBroadcast(OffenseCurrentGoalOffset) * 10000);
									goal = RobotUtil.intToMapLoc(rc.readBroadcast(rc.readBroadcast(OffenseCurrentGoalOffset)));
									System.out.println("New goal recieved at "+goal);
								}
								//if the goal pastr is gone tell the hq
								if(rc.canSenseSquare(goal)){
									if(rc.senseObjectAtLocation(goal) == null){
										rc.broadcast(OffenseGoalDestroyed, 1);
										if(enemiesNear.size() > 0){
											Direction moveDirection = currentLocation.directionTo(rc.senseLocationOf(enemiesNear.get(rand.nextInt() % enemiesNear.size())));
											RobotUtil.moveInDirection(rc, moveDirection, "move");
										}else{
											int counter = 0;
											for(int channel: OffenseGoalLocations){
												if(rc.readBroadcast(channel) > 0){
													counter++;
												}
											}
											if(counter < 2){//if there is not another channel go to ours
												goal = RobotUtil.intToMapLoc(rc.readBroadcast(DefenseGoalLocation));
												map = RobotUtil.readMapFromBroadcast(rc, DefenseChannelOffset);
												wasOffense = true;
												robotMission = missions.defense;
											}
										}
									}
								}
								//move towards goal
								int intToGoal = map[currentLocation.x][currentLocation.y] - 1;
								Direction dirToGoal = directions[intToGoal];
								RobotUtil.moveInDirection(rc, dirToGoal, "move");
							}
						}
					}
				} catch (Exception e) {
					//e.printStackTrace();
				}
			} else if (rc.getType() == RobotType.NOISETOWER) {
				try {
                    if(rc.isActive()){
                        currentLocation = rc.getLocation();
                        int maxAttack = (int)Math.sqrt(rc.getType().attackRadiusMaxSquared);
                        for (int j = 0; j < 1080; j += 45) {
                            for(int i = maxAttack; i > 2; i-=1){
                                if(rc.isActive()){
                                    double xVal = Math.cos(j * Math.PI / 180.0) * i;
                                    double yVal = Math.sin(j * Math.PI / 180.0) * i;

                                    MapLocation squareToAttack = currentLocation.add((int)xVal, (int)yVal);
                                    if(rc.canAttackSquare(squareToAttack) &&
                                            squareToAttack.x < rc.getMapWidth() + 3 && squareToAttack.x > -3 &&
                                            squareToAttack.y < rc.getMapHeight() + 3 && squareToAttack.y > -3) {
                                        rc.attackSquare(squareToAttack);
                                        rc.yield();
                                        rc.yield();
                                    }
                                }
                            }
                        }
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
			
			rc.yield();
		}
        
	}
}
