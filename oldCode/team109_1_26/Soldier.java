package team109_1_26;

import java.util.Random;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameObject;
import battlecode.common.MapLocation;
import battlecode.common.Robot;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Team;
import battlecode.common.Upgrade;

public class Soldier extends MyRobot{
	
	private static RobotController rc;
	private static MapLocation enemyHQ, allyHQ;
	private static Team myTeam, enemyTeam;
	private static int defuseCost;
	private static int dissenter;
	private static int distToEnemy_2,distToAlly_2;
	private static int round;
	private static MapLocation prevLocation;
	private static MapLocation currLocation;
	private static MapLocation[] encampments;
	private static int HQSeparation;
	private static Random rand;
	private static Robot[] enemyRobots;
	private static int seed;
	private static int mapHeight, mapWidth, mapDiag_2;
	private static boolean wftb;//weflankinthisbitch
	private static boolean nonoGoAhead;//a boolean telling robots to NOT set a mine after spawning
	private static MapLocation[] badmines;
	
	/*
	 * Code for a miner/mine sweeper?
	 * Code for dissenter
	 */
	
	protected static void run(RobotController myRc){
		
		rc = myRc;
		enemyHQ = rc.senseEnemyHQLocation();
		allyHQ = rc.senseHQLocation();
		myTeam = rc.getTeam();
		enemyTeam = myTeam.opponent();
		defuseCost = 12;
		prevLocation = null;
		currLocation = rc.getLocation();
		HQSeparation = allyHQ.distanceSquaredTo(enemyHQ);
		seed = rc.getRobot().getID();
		rand = new Random(seed);
		mapHeight = rc.getMapHeight();
		mapWidth = rc.getMapWidth();
		mapDiag_2 = mapHeight * mapHeight + mapWidth * mapWidth;
		nonoGoAhead = false;
		wftb=false;
		
		try {
			if(rc.readBroadcast(getChannel() + 1) == 4){
				strictRush();
			} else {
				normalRun();
			}
		} catch (GameActionException e1) {
			e1.printStackTrace();
		}
	}
	
	private static void strictRush(){
		System.out.println("Strict rush");
		MapLocation rallyPoint = findRallyPoint();
		while(true){
			try{
				if(rc.isActive()){
					Robot[] enemyRobots = rc.senseNearbyGameObjects(Robot.class, 14, enemyTeam);
					Robot[] allyRobots = rc.senseNearbyGameObjects(Robot.class, 14, myTeam);
					currLocation = rc.getLocation();
					if(Clock.getRoundNum() > 201){
						if(enemyRobots.length == 0){
							moveTo(enemyHQ);
						} else {
							moveTo(findClosest(enemyRobots));
						}
					} else {
						boolean tooFar = currLocation.distanceSquaredTo(enemyHQ) > currLocation.distanceSquaredTo(allyHQ);
						if(enemyRobots.length != 0 && (allyRobots.length > 7 || allyRobots.length >= enemyRobots.length)
								&& !tooFar){
							moveTo(findClosest(enemyRobots));
						} else if (tooFar){
							moveTo(rallyPoint);
						} else {
							if(rc.senseMine(currLocation) == null)
								rc.layMine();
							else {
								Direction dir = rand.nextInt(10) < 5 ? currLocation.directionTo(enemyHQ) 
										 : directions[rand.nextInt(63) / 8];
								moveTo(currLocation.add(dir));
							}
						}
					}
				}
				
			} catch (Exception e){
				System.out.println("Strict Rush Error");
				e.printStackTrace();
				rc.breakpoint();
			}
			rc.yield();
		}
	}
	
	private static MapLocation findRallyPoint() {
		return new MapLocation(	(enemyHQ.x + 3 * allyHQ.x) / 4, 
								(enemyHQ.y + 3 * allyHQ.y) / 4);
	}
	
	private static void normalRun(){
		System.out.println("Normal run");
		if(Clock.getRoundNum() < 150)//first 150 rounds
			initialRush(200);
		if(Clock.getRoundNum() < 350)
			initialRoam(350);
		
		dissenter = rand.nextInt(100);
		
		while(true){
			try{
				int channel = getChannel();

				int ir = rc.readBroadcast(channel + 1);
				if (ir > 0 && ir < 3) {
					mode = ir;
				}
				
				if(defuseCost > 5 && rc.hasUpgrade(Upgrade.DEFUSION))
					defuseCost = 5;
				
				if(rc.isActive()){
					round = Clock.getRoundNum();
					distToAlly_2 = currLocation.distanceSquaredTo(allyHQ);
					distToEnemy_2 = currLocation.distanceSquaredTo(enemyHQ);
					enemyRobots = rc.senseNearbyGameObjects(Robot.class, 1000000, enemyTeam);
					currLocation = rc.getLocation();
					if(round<500)defaultStrategy(false);
					else if(round<600)defaultStrategy(true);
					else /*if(round<1200)*/defaultStrategy(false);
					//else letsMakeANuke();
					
				}
				
			} catch (Exception e){
				System.out.println("Soldier Error");
				e.printStackTrace();
				rc.breakpoint();
			}
			rc.yield();
		}
	}
	
	private static MapLocation findClosest(Robot[] enemyRobots)
			throws GameActionException {
		int closestDist = 1000000;
		MapLocation closestEnemy = null;
		for (int i = 0; i < enemyRobots.length; i++) {
			Robot arobot = enemyRobots[i];
			if(rc.canSenseObject(arobot)){
				RobotInfo arobotInfo = rc.senseRobotInfo(arobot);
				int dist = arobotInfo.location.distanceSquaredTo(rc.getLocation());
				if (dist < closestDist) {
					closestDist = dist;
					closestEnemy = arobotInfo.location;
				}
			}
		}
		return closestEnemy;
	}
	private static MapLocation findClosest(MapLocation[] points) 
			throws GameActionException{
		MapLocation cur = rc.getLocation();
		int close=999999,closestInd=-1;
		for(int i=0;i<points.length;i++)
		{
			int dis=cur.distanceSquaredTo(points[i]);
			if(dis<close){close=dis;closestInd=i;}
		}
		if(closestInd==-1)return cur;
		else return points[closestInd];
	}
	
	/*
	 * Rushes to go capture encampments
	 */
	private static void initialRush(int clockLim){
		MapLocation target;
		mode = 1;
		
		try{
			/*badmines=rc.senseNonAlliedMineLocations(allyHQ,12);
			while((badmines = rc.senseNonAlliedMineLocations(allyHQ,12)).length > 0)
				{
					moveTo(findClosest(badmines));
					rc.yield();
				}*/
			encampments = rc.senseEncampmentSquares(currLocation, 1000, Team.NEUTRAL);
			target = setEncampmentTarget(null);
				while(target != null && Clock.getRoundNum() < clockLim){
					if(defuseCost > 5 && rc.hasUpgrade(Upgrade.DEFUSION))
						defuseCost = 5;
					
					target = rushCode(target);
					
					rc.yield();
				}
			
		} catch (Exception e){
			System.out.println("Rush Error");
			e.printStackTrace();
			rc.breakpoint();
		}
		
		mode = 0;
	}
	
	private static MapLocation rushCode(MapLocation target) throws GameActionException{
		if(rc.isActive()){
			if(rc.canSenseSquare(target)){
				Robot r = (Robot) rc.senseObjectAtLocation(target);
				if(r != null && !r.equals(rc.getRobot())){
					encampments = rc.senseEncampmentSquares(currLocation, 1000, Team.NEUTRAL);
					target = setEncampmentTarget(null);
				}
			}
			moveTo(target);
		}
		return target;
	}
	//Looks around randomly while laying mines and killing any enemy robots in sight.*********************
	private static void initialRoam(int clockLim){
		currLocation = rc.getLocation();
		mode = 3;
		while(Clock.getRoundNum() < clockLim){
			try{
				if(defuseCost > 5 && rc.hasUpgrade(Upgrade.DEFUSION))
					defuseCost = 5;
				
				if(rc.isActive()){
					Robot[] enemyRobots = rc.senseNearbyGameObjects(Robot.class, 1000000, enemyTeam);
					
					if(enemyRobots.length != 0){
						int temp = mode;
						mode = 0;
						moveTo(findClosest(enemyRobots));
						mode = temp;
					} else {
						if(currLocation.distanceSquaredTo(enemyHQ) <= HQSeparation + 8 &&
							rc.senseMine(currLocation) == null){
							rc.layMine();
						} else {
							//Choose a random direction, and move that way if possible
							Direction dir = rand.nextInt(10) < 5 ? currLocation.directionTo(enemyHQ) 
															 	 : directions[rand.nextInt(63) / 8];
							moveTo(currLocation.add(dir));
						}	
					}
					
				}
			} catch (Exception e){
				System.out.println("Roam Error");
				e.printStackTrace();
				rc.breakpoint();
			}
			rc.yield();
		}
		mode = 0;
	}
	
	/*
	 * finds the nearest encampment for use
	 */
	private static void defaultStrategy(boolean defense) throws GameActionException
	{
		if(dissenter > 15){
			if(enemyRobots.length == 0){ // No nearby enemies
				//If we have a big enough possy to go "exploring" or are too close to our HQ
				if(currLocation.distanceSquaredTo(allyHQ) < 13)
					moveTo(enemyHQ);
				else if(rc.senseNearbyGameObjects(Robot.class, 30, myTeam).length > (round < 501 ? 5 : 10))
					if(defense && !nonoGoAhead && rc.senseMine(currLocation) == null && distToEnemy_2<allyHQ.distanceSquaredTo(enemyHQ)+10)
						{rc.layMine();nonoGoAhead=true;}
					else
					moveTo(enemyHQ);
				 else {
					if(distToAlly_2 * 10 < mapDiag_2){
						moveTo(allyHQ);
					} else if(distToEnemy_2 * 10 < mapDiag_2){
						moveTo(enemyHQ);
					} else {
						if(distToEnemy_2 <= HQSeparation && rc.senseMine(currLocation) == null){
							rc.layMine();
						} else {
							Direction dir = rand.nextInt(10) < 5 ? currLocation.directionTo(enemyHQ) 
									 : directions[rand.nextInt(63) / 8];
							moveTo(currLocation.add(dir));
						}
					}
				}
			} else {// found an enemy
				int temp = mode;
				mode = 0;
				moveTo(findClosest(enemyRobots));
				mode = temp;
			}
		} else if (dissenter > 8){
			encampments = rc.senseEncampmentSquares(rc.getLocation(), 1000, Team.NEUTRAL);
			MapLocation target = setEncampmentTarget(null);
			int temp = mode;
			mode = 1;
			while(target != null){
				if(defuseCost > 5 && rc.hasUpgrade(Upgrade.DEFUSION))
					defuseCost = 5;
				
				target = rushCode(target);
				
				rc.yield();
			}
			mode = temp;
		} else {
			if(rc.senseMine(currLocation)==null && currLocation.distanceSquaredTo(enemyHQ) <= HQSeparation && rc.senseMine(currLocation) == null){
				rc.layMine();
			} else {
				Direction dir = rand.nextInt(10) < 5 ? currLocation.directionTo(enemyHQ) 
						 : directions[rand.nextInt(63) / 8];
				moveTo(currLocation.add(dir));
			}
		}
	}
	private static MapLocation setEncampmentTarget(MapLocation prevTarget) throws GameActionException{
		
		MapLocation ret = null;
		int distance = 1000000;
		
		for(MapLocation loc : encampments){//Each iteration at worst 2050 bytecode

			if(Clock.getBytecodesLeft() < 3500)//Ensures that the turn can be finished
				break;
			
			if(loc.equals(prevTarget)){
				continue;
			} else if(rc.canSenseSquare(loc)){
				GameObject obj = rc.senseObjectAtLocation(loc);
				if((obj != null && obj.getTeam() == myTeam) || hqClustered(loc))
					continue;
			}
			
			int temp = loc.distanceSquaredTo(currLocation);
			if(distance > temp){
				distance = temp;
				ret = loc;
			}
		}
		
		if(ret != null){//About 60 bytecode
			setEncampmentType(ret.distanceSquaredTo(allyHQ), ret.distanceSquaredTo(enemyHQ));
			rc.setIndicatorString(0, "" + encampmentType);
		}
		
		return ret;
	}
	
	/*
	 * Costs ~60 bytecode
	 */
	private static void setEncampmentType(int dist_to_aHQ_2, int dist_to_eHQ_2){
		if(	(dist_to_aHQ_2 < Math.min(HQSeparation/2, 63)) ||
			(dist_to_eHQ_2 < Math.min(HQSeparation/2, 63)))
			encampmentType = RobotType.ARTILLERY;
		else if (dist_to_eHQ_2 > dist_to_aHQ_2 + HQSeparation){//Behind HQ to enemy HQ
			encampmentType = rand.nextInt(10) < 5 ? RobotType.SUPPLIER : RobotType.GENERATOR;
		} else {
			int random = rand.nextInt(3);
			switch(random){
				case 0: encampmentType = RobotType.ARTILLERY; 	break;
				case 1: encampmentType = RobotType.SUPPLIER;	break;
				case 2: encampmentType = RobotType.GENERATOR;	break;	
			}
		}
			
			
	}
	
	/*
	 * Worst Case BYTECODE Cost: 2000
	 */
	private static boolean hqClustered(MapLocation loc) throws GameActionException{
		if(!allyHQ.isAdjacentTo(loc))
			return false;
		
		//Counts number of encampments currently next to HQ
		Robot[] robots = rc.senseNearbyGameObjects(Robot.class, allyHQ, 3, myTeam);
		int numEncamp = 0;
		for(Robot r : robots){
			if(r.equals(rc.getRobot()))
				continue;
			numEncamp ++;
		}
		
		//Counts how many non-damaging squares adjacent to HQ 
		int openSpace = 0;
		for(int i = -1; i < 2; i++){
			for(int j = -1; j < 2; j++){
				if(!inRange(allyHQ.x + i, allyHQ.y + j) || (i == 0 && j == 0))
					continue;
				
				Team mine = rc.senseMine(allyHQ.add(i, j));
				if(mine == null || mine == myTeam)
					openSpace++;
			}
		}

		return numEncamp*2 >=  openSpace;
	}
	
	/*
	 * makes sure that a target is within memory bounds
	 */
	private static boolean inRange(int x, int y) {
		return x > -1 && y > -1 && x < rc.getMapWidth() && y < rc.getMapHeight();
	}
	
	/*
	 * Ensures that there is enough power for the rest of the team to move if capturing
	 */
	private static boolean enoughPowerToCapture(){
		return rc.senseCaptureCost() < 
				(rc.getTeamPower() - rc.senseNearbyGameObjects(Robot.class, 1000000, myTeam).length * 2); 
	}

	/*
	 * Moves to a location. If mode is for capturing, captures.
	 * if mode is for defusing, defuses. For now assume
	 * Mode 0 = attack   	-> requires adjacent
	 * mode 1 = capture		-> requires on top of
	 * mode 2 = defuse mine	-> requires adjacent
	 * mode 3 = plant mine	-> requires on top of
	 * 
	 * WORST CASE BYTECODE COST: ~1600 (with goToLocation code included)
	 * 
	 * NOTE: the current algorithm has the units park if target is reached instead of cycle around randomly.
	 * Before the cycling did less damage but could potentially save units (though only very slightly)
	 * from sustaining damage. Cycling around the target specifically would have a mix of both effects,
	 * but could cost lots of bytecode. Also, the current goToLocation method is best with group movement
	 * and awful for singular movement. Will need to tweek the heuristics to check some more.... HMMMMM
	 */
	private static int mode;
	private static RobotType encampmentType;
	private static void moveTo(MapLocation target) throws GameActionException{
		if(target == null)
			return;
		
		currLocation = rc.getLocation();
		Team mine = rc.senseMine(currLocation);
		if(mine == enemyTeam){//Enemy mine underneath -> MOVE OFF
			goToLocation(target, true);
		} else if(mode % 2 == 0){//Attack | defuse modes
			if(currLocation.isAdjacentTo(target)){//Target is in front
				if(mode == 0)
					return;
				else
					rc.defuseMine(target);
			} else {//Target is far away
				goToLocation(target, false);
			}
		} else {//capture | plant modes
			if(currLocation.equals(target)){//at location
				if(mode == 1 && enoughPowerToCapture()){
					rc.captureEncampment(encampmentType);
				} else if(mine == null){
					rc.layMine();
				}
			} else {//target is far away
				goToLocation(target, false);
			}
		}
	}
	
	/*
	 * Tries to make a move into the next location. If currently on a mine, then
	 * allow movement in all directions, else only allow movement in 5 specific directions
	 * 
	 * WORST CASE BYTECODE COST: 1478
	 * 
	 * UPDATES and BYTECODE added from update: 
	 * Include Manhattan Distance in comparison		+ 80
	 * Prevent movement to previous square			+100
	 * Allow backwards movement if already on mine	+350 //because 8 directions can be checked instead of 5 directions
	 * 
	 * NOTES:
	 * Unsure how algorithm will work when taking into account mass movement. However
	 * there are many upgrades that can be done.
	 */
	private static void goToLocation(MapLocation target, boolean hasMine) throws GameActionException{
		Direction dir = findBestPath(target, hasMine ? ALL_DIRS : DIRS);
		if(dir != null){
			MapLocation ahead = (prevLocation = currLocation).add(dir);
			Team m = rc.senseMine(ahead);
			if(m != null && m != myTeam){
				rc.defuseMine(ahead);
			} else{
				rc.move(dir);
			}
		}
	}
	
	//All directions
	private static Direction[] directions = new Direction[]{
		Direction.NORTH, Direction.NORTH_EAST, Direction.EAST, Direction.SOUTH_EAST,
		Direction.SOUTH, Direction.SOUTH_WEST, Direction.WEST, Direction.SOUTH_WEST
	};
	
	//Direction cycling options
	private static final int[] DIRS 	= new int[]{0, 1, -1, 2, -2};
	private static final int[] ALL_DIRS = new int[]{0, 1, -1, 2, -2, 3, -3, 4};
	
	/*
	 * Finds the best next move
	 */
	private static Direction findBestPath(MapLocation target, int[] allowedSteps){
		Direction dir = currLocation.directionTo(target);
		Direction lookAt = dir;

		Node first = null;
		for(int d: allowedSteps){
			lookAt = directions[(dir.ordinal() + d + 8) % 8];
			MapLocation next = currLocation.add(lookAt);
			if(rc.canMove(lookAt) && !next.equals(prevLocation)){
				Node temp = new Node(currLocation.add(lookAt), target, lookAt);
				if(first == null || first.compareTo(temp) > 0)
					first = temp;
			}
		}

		if(first == null){
			return null;
		}
		
		return first.temp;
	}
	
	private static class Node{
		
		public int cost;
		public int cHeuristic, mHeuristic;
		public Team mine;
		public Direction temp;
		
		public Node(MapLocation l, MapLocation target, Direction d){
			temp = d;
			mine = rc.senseMine(l);
			int dx = Math.abs(l.x - target.x), dy = Math.abs(l.y - target.y);
			cHeuristic = Math.max(dx, dy);//Chebyshev heuristic
			mHeuristic = dx + dy; //Manhattan Heuristic
			cost = ((mine != null && mine != myTeam) ? defuseCost : 0);
		}
		
		/*
		 * if the Chebyshev heuristic difference is 0, then check by Manhattan heuristic
		 */
		public int compareTo(Node p){
			return cost - p.cost + 
					(cHeuristic == p.cHeuristic ? mHeuristic - p.mHeuristic: cHeuristic - p.cHeuristic);
		}
	}
}
