package dev.horizon.leadseal;

/**
 * A part of a migration that renders SQL. Operations and the change that groups them are nodes.
 *
 * <p>
 * Each node renders standard SQL. A node renders its child nodes through {@link DdlWriter#render}, so the dialect's
 * {@link Decorator} can rewrite every child before it renders.
 */
@FunctionalInterface
public interface Node {

  /**
   * Renders this node.
   *
   * @param sql the writer that receives the SQL
   */
  void render(DdlWriter sql);
}
