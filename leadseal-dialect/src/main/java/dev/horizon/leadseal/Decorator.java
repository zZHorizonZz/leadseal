package dev.horizon.leadseal;

import java.util.function.Function;

/**
 * Rewrites nodes before they render. Dialects use decorators to write DDL that differs from standard SQL.
 *
 * <p>
 * {@link DdlWriter#render} calls the decorator once for each node, starting with the {@link Change}. The writer
 * renders the returned node without decorating it again.
 */
@FunctionalInterface
public interface Decorator {

  /**
   * Rewrites a node.
   *
   * @param node the node to rewrite
   * @return the node to render, which can be {@code node} itself
   */
  Node decorate(Node node);

  /**
   * Returns a decorator that applies this decorator and then {@code next}.
   *
   * @param next the decorator to apply second
   * @return the combined decorator
   */
  default Decorator then(Decorator next) {
    return node -> next.decorate(decorate(node));
  }

  /**
   * Returns a decorator that rewrites nodes of one type and returns all other nodes unchanged.
   *
   * @param type the node type to rewrite
   * @param rewrite the function that rewrites a node of that type
   * @param <N> the node type
   * @return the decorator
   */
  static <N extends Node> Decorator of(Class<N> type, Function<? super N, ? extends Node> rewrite) {
    return node -> type.isInstance(node) ? rewrite.apply(type.cast(node)) : node;
  }

  /**
   * Returns a decorator that returns every node unchanged.
   *
   * @return the decorator
   */
  static Decorator none() {
    return node -> node;
  }
}
