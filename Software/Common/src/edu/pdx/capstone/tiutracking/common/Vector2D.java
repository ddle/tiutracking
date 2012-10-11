package edu.pdx.capstone.tiutracking.common;

import java.io.Serializable;

public class Vector2D implements Serializable {

	private static final long serialVersionUID = -4810635128540403129L;
	
	public double x, y;

	public Vector2D() {
		this.x = 0;
		this.y = 0;
	}

	public Vector2D(double x, double y) {
		this.x = x;
		this.y = y;
	}

	public Vector2D(Vector2D v) {
		this.x = v.x;
		this.y = v.y;
	}

	public void set(double x, double y) {
		this.x = x;
		this.y = y;
	}
	
	public void set(Vector2D v) {
		this.x = v.x;
		this.y = v.y;
	}
	
	public void add(double x, double y) {
		this.x += x;
		this.y += y;
	}

	public void add(Vector2D v) {
		this.x += v.x;
		this.y += v.y;
	}

	public void sub(double x, double y) {
		this.x -= x;
		this.y -= y;
	}

	public void sub(Vector2D v) {
		this.x -= v.x;
		this.y -= v.y;
	}

	public void mult(double sx, double sy) {
		this.x *= sx;
		this.y *= sy;
	}

	public void mult(double c) {
		this.x *= c;
		this.y *= c;
	}

	public void lerp(Vector2D other, double factor) {
		this.x += (other.x - this.x) * factor;
		this.y += (other.y - this.y) * factor;
	}

	public void lerp(Vector2D a, Vector2D b, double factor) {
		this.x = a.x + (b.x - a.x) * factor;
		this.y = b.y + (b.y - a.y) * factor;
	}

	public double distanceTo(Vector2D v) {
		double dx = this.x - v.x;
		double dy = this.x - v.y;
		return Math.sqrt(dx * dx + dy * dy);
	}

	public double mag() {
		if (x == 0 && y == 0) {
			return 0;
		}
		return Math.sqrt(x * x + y * y);
	}

	public double magSquared() {
		return x * x + y * y;
	}
	
	public String toString() {
		return "(x: " + x + ", y: " + y + ")";
	}

	public static Vector2D sub(Vector2D a, Vector2D b) {
		return new Vector2D(a.x - b.x, a.y - b.y);
	}

	public static Vector2D mult(Vector2D v, double c) {
		return new Vector2D(v.x * c, v.y * c);
	}

}