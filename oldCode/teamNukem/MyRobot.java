package teamNukem;

import battlecode.common.Clock;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.Robot;
import battlecode.common.RobotController;

public abstract class MyRobot {

	protected static int hashChannel(int x, int y, int r){
		x /= 10;
		y /= 10;
		int d1 = r % 6;
		int d2 = r * r % 100;
		StringBuilder b = new StringBuilder();
		b.append(d1);
		b.append(x);
		b.append(y);
		b.append(String.format("%02d", d2));
		return Integer.parseInt(b.toString());
	}
	
	protected static int getDistressMessage(MapLocation l, int priority) {
		StringBuilder b = new StringBuilder();
		b.append(1);// Security #
		b.append(priority);// Priority of Message
		b.append(3);// # Soldiers needed
		b.append(String.format("%02d", l.x));// Map Location as Int
		b.append(String.format("%02d", l.y));// Map Location as Int
		b.append(1);// Soldier mode
		b.append(47);// Security #

		return Integer.parseInt(b.toString());
	}
	
	protected static void distressSignal(RobotController rc, int priority) throws GameActionException{
		//Get nearby enemies
		Robot[] enemies = rc.senseNearbyGameObjects(Robot.class, 33,
				rc.getTeam().opponent());
		
		if (enemies.length > 0) {//If enemies nearby
			int channel = hashChannel(rc.getLocation().x, rc.getLocation().y, Clock.getRoundNum());
			int currBroadcast = rc.readBroadcast(channel);
			
			//if security digits don't match or the original priority is lower
			if((currBroadcast % 100 != 47 && currBroadcast/1000000000 != 1) || 
					currBroadcast / 100000000 % 10 < priority){
				rc.broadcast(channel, getDistressMessage(rc.senseLocationOf(enemies[0]), priority));
				rc.setIndicatorString(0, "Posted Distress Signal");
			}
			
		} else {
			rc.setIndicatorString(0, "No problem");
		}
	}
	
	/*************************************HQ Specific Channels*************************************/
	
	protected static int getEmergencyChannel(int r){
		int d1 = r % 6;
		int d2 = r * r % 100;
		int d3 = 80 + r % 20;
		StringBuilder b = new StringBuilder();
		b.append(d1);
		b.append(String.format("%02d", d3));
		b.append(String.format("%02d", d2));
		return Integer.parseInt(b.toString());
	}
	
	protected static int attackChannel1(int r){
		int d1 = r % 6;
		int d2 = r * r % 100;
		StringBuilder b = new StringBuilder();
		b.append(d1);
		b.append(String.format("%02d", d2));
		b.append(d1);
		b.append(3);
		return Integer.parseInt(b.toString());
	}
	
	protected static int attackChannel2(int r){
		int d1 = r % 6;
		int d2 = r * r % 100;
		StringBuilder b = new StringBuilder();
		b.append(d1);
		b.append(String.format("%02d", d2));
		b.append(d1);
		b.append(7);
		return Integer.parseInt(b.toString());
	}
	
	/*************************************HQ Specific Channels*************************************/
}
