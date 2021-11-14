package com.gitlab.aecsocket.minecommons.paper.command;

import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.arguments.CommandArgument;
import cloud.commandframework.arguments.parser.ArgumentParseResult;
import cloud.commandframework.arguments.parser.ArgumentParser;
import cloud.commandframework.captions.Caption;
import cloud.commandframework.captions.CaptionVariable;
import cloud.commandframework.context.CommandContext;
import cloud.commandframework.exceptions.parsing.NoInputProvidedException;
import cloud.commandframework.exceptions.parsing.ParserException;
import com.gitlab.aecsocket.minecommons.core.Duration;
import net.kyori.adventure.key.Key;
import org.checkerframework.checker.nullness.qual.NonNull;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.function.BiFunction;

/**
 * Command argument which parses a {@link Duration}.
 * @param <C> The command sender type.
 */
public final class DurationArgument<C> extends CommandArgument<C, Duration> {
    /** When a duration cannot be parsed. */
    public static final Caption ARGUMENT_PARSE_FAILURE_DURATION = Caption.of("argument.parse.failure.duration");

    private DurationArgument(
            final boolean required,
            final @NonNull String name,
            final @NonNull String defaultValue,
            final @Nullable BiFunction<@NonNull CommandContext<C>,
                    @NonNull String, @NonNull List<@NonNull String>> suggestionsProvider,
            final @NonNull ArgumentDescription defaultDescription
    ) {
        super(required, name, new DurationParser<>(), defaultValue, Duration.class, suggestionsProvider, defaultDescription);
    }

    /**
     * Create a new builder
     *
     * @param name   Name of the component
     * @param <C>    Command sender type
     * @return Created builder
     */
    public static <C> @NonNull Builder<C> newBuilder(final @NonNull String name) {
        return new Builder<>(name);
    }

    /**
     * Create a new required command component
     *
     * @param name   Component name
     * @param <C>    Command sender type
     * @return Created component
     */
    public static <C> @NonNull CommandArgument<C, Duration> of(final @NonNull String name) {
        return DurationArgument.<C>newBuilder(name).asRequired().build();
    }

    /**
     * Create a new optional command component
     *
     * @param name   Component name
     * @param <C>    Command sender type
     * @return Created component
     */
    public static <C> @NonNull CommandArgument<C, Duration> optional(final @NonNull String name) {
        return DurationArgument.<C>newBuilder(name).asOptional().build();
    }

    /**
     * Create a new required command component with a default value
     *
     * @param name         Component name
     * @param defaultValue Default value
     * @param <C>          Command sender type
     * @return Created component
     */
    public static <C> @NonNull CommandArgument<C, Duration> optional(
            final @NonNull String name,
            final @NonNull Key defaultValue
    ) {
        return DurationArgument.<C>newBuilder(name).asOptionalWithDefault(defaultValue.toString()).build();
    }

    /**
     * Builder class.
     * @param <C> The command sender type.
     */
    public static final class Builder<C> extends CommandArgument.Builder<C, Duration> {
        private Builder(final @NonNull String name) {
            super(Duration.class, name);
        }

        /**
         * Builder a new example component
         *
         * @return Constructed component
         */
        @Override
        public @NonNull DurationArgument<C> build() {
            return new DurationArgument<>(
                    this.isRequired(),
                    this.getName(),
                    this.getDefaultValue(),
                    this.getSuggestionsProvider(),
                    this.getDefaultDescription()
            );
        }
    }

    /**
     * Parser class.
     * @param <C> The command sender type.
     */
    public static final class DurationParser<C> implements ArgumentParser<C, Duration> {
        @Override
        public @NonNull ArgumentParseResult<Duration> parse(
                final @NonNull CommandContext<C> ctx,
                final @NonNull Queue<@NonNull String> inputQueue
        ) {
            final String input = inputQueue.peek();
            if (input == null) {
                return ArgumentParseResult.failure(new NoInputProvidedException(
                        Key.class,
                        ctx
                ));
            }
            inputQueue.remove();

            try {
                return ArgumentParseResult.success(Duration.duration(input));
            } catch (IllegalArgumentException e) {
                return ArgumentParseResult.failure(new ParseException(input, ctx, e));
            }
        }

        @Override
        public boolean isContextFree() {
            return true;
        }

        @Override
        public @NonNull List<@NonNull String> suggestions(@NonNull CommandContext<C> ctx, @NonNull String input) {
            return Collections.emptyList();
        }
    }

    /**
     * Exception type.
     */
    public static final class ParseException extends ParserException {
        /**
         * Creates an instance.
         * @param input The input.
         * @param ctx The context.
         * @param e The exception.
         */
        public ParseException(String input, CommandContext<?> ctx, Exception e) {
            super(Key.class, ctx, ARGUMENT_PARSE_FAILURE_DURATION,
                    CaptionVariable.of("input", input),
                    CaptionVariable.of("error", e.getMessage()));
        }
    }
}
