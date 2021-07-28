package com.gitlab.aecsocket.minecommons.core.raycast;

import com.gitlab.aecsocket.minecommons.core.bounds.Bound;
import com.gitlab.aecsocket.minecommons.core.vector.cartesian.Ray3;
import com.gitlab.aecsocket.minecommons.core.vector.cartesian.Vector3;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Collection;
import java.util.function.Predicate;

/**
 * Provides raycasting in a 3D space, colliding with defined boundables.
 * @param <B> The boundable type.
 */
public abstract class Raycast<B extends Boundable> {

    /**
     * A collision result.
     * @param <B> The boundable type.
     * @param ray The ray used to get this result.
     * @param distance The distance travelled from the origin to the in-position.
     * @param in Either the final position, or the position that the ray entered a bound.
     * @param out The position that the ray exited a bound, or null if no bound was hit.
     * @param normal The normal of the surface hit.
     * @param penetration The distance travelled through the bound, or -1 if no bound was hit.
     * @param hit The bound that was hit, or null if no bound was hit.
     */
    public record Result<B extends Boundable>(
            Ray3 ray,
            double distance,
            Vector3 in,
            @Nullable Vector3 out,
            @Nullable Vector3 normal,
            double penetration,
            @Nullable B hit
    ) {}

    /**
     * Checks if a ray intersects with an object.
     * @param ray The ray.
     * @param object The object.
     * @param test The test which determines if an object is eligible to be intersected.
     * @return The intersection result, or null if the object did not intersect.
     */
    protected @Nullable Result<B> intersects(Ray3 ray, B object, @Nullable Predicate<B> test) {
        if (test != null && !test.test(object))
            return null;
        Vector3 orig = object.origin();
        ray = ray.at(ray.orig().subtract(orig));
        Bound.Collision collision = object.bound().collision(ray);
        if (collision != null) {
            return new Result<>(ray, collision.in(),
                    ray.point(collision.in()).add(orig),
                    ray.point(collision.out()).add(orig),
                    collision.normal(),
                    collision.out() - collision.in(),
                    object);
        }
        return null;
    }

    /**
     * Checks if a ray intersects with any of the objects provided.
     * @param ray The ray.
     * @param objects The set of objects object.
     * @param test The test which determines if an object is eligible to be intersected.
     * @return The intersection result, or null no object intersected.
     */
    protected @Nullable Result<B> intersects(Ray3 ray, Collection<? extends B> objects, @Nullable Predicate<B> test) {
        for (B object : objects) {
            var result = intersects(ray, object, test);
            if (result != null)
                return result;
        }
        return null;
    }

    /**
     * Tests for intersections between the provided ray, and the objects determined by this raycast.
     * @param ray The ray.
     * @param maxDistance The max distance the ray will travel.
     * @param test The test which determines if an object is eligible to be intersected.
     * @return The cast result.
     */
    public abstract Result<B> cast(Ray3 ray, double maxDistance, @Nullable Predicate<B> test);


    /**
     * Tests for intersections between the provided ray, and the objects determined by this raycast.
     * @param origin The start location.
     * @param direction The ray direction.
     * @param maxDistance The max distance the ray will travel.
     * @param test The test which determines if an object is eligible to be intersected.
     * @return The cast result.
     */
    public Result<B> cast(Vector3 origin, Vector3 direction, double maxDistance, @Nullable Predicate<B> test) {
        return cast(Ray3.ray3(origin, direction), maxDistance, test);
    }
}
