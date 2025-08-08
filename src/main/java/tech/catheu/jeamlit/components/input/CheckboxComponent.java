package tech.catheu.jeamlit.components.input;

import com.fasterxml.jackson.core.type.TypeReference;
import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import tech.catheu.jeamlit.core.JtComponent;
import tech.catheu.jeamlit.core.JtComponentBuilder;

import java.io.StringWriter;
import java.util.function.Consumer;

public class CheckboxComponent extends JtComponent<Boolean> {
    // Protected fields for Mustache template visibility
    protected final String label;
    protected final boolean value;
    protected final @Nullable String help;
    protected final boolean disabled;
    protected final LabelVisibility labelVisibility;
    protected final String width;

    private static final Mustache registerTemplate;
    private static final Mustache renderTemplate;

    static {
        final MustacheFactory mf = new DefaultMustacheFactory();
        registerTemplate = mf.compile("components/input/CheckboxComponent.register.html.mustache");
        renderTemplate = mf.compile("components/input/CheckboxComponent.render.html.mustache");
    }

    private CheckboxComponent(Builder builder) {
        super(builder.generateKeyForInteractive(), builder.value, builder.onChange);

        this.label = builder.label;
        this.value = builder.value;
        this.help = builder.help;
        this.disabled = builder.disabled;
        this.labelVisibility = builder.labelVisibility;
        this.width = builder.width;
    }

    public static class Builder extends JtComponentBuilder<Boolean, CheckboxComponent, Builder> {
        private final String label;
        private boolean value = false;
        private @Nullable String help = null;
        private boolean disabled = false;
        private LabelVisibility labelVisibility = LabelVisibility.VISIBLE;
        private String width = "content";
        private @Nullable Consumer<Boolean> onChange;

        public Builder(@Nonnull String label) {
            if (label == null || label.trim().isEmpty()) {
                throw new IllegalArgumentException("Label cannot be null or empty for accessibility reasons");
            }
            this.label = label;
        }

        public Builder value(boolean value) {
            this.value = value;
            return this;
        }

        public Builder help(@Nullable String help) {
            this.help = help;
            return this;
        }

        public Builder disabled(boolean disabled) {
            this.disabled = disabled;
            return this;
        }

        public Builder labelVisibility(@Nonnull LabelVisibility labelVisibility) {
            this.labelVisibility = labelVisibility;
            return this;
        }

        public Builder width(@Nonnull String width) {
            // Validate width parameter according to Streamlit spec
            if (!width.equals("content") && !width.equals("stretch") && !width.matches("\\d+")) {
                throw new IllegalArgumentException(
                        "width must be 'content', 'stretch', or a pixel value (integer). Got: " + width);
            }
            this.width = width;
            return this;
        }

        public Builder onChange(@Nullable Consumer<Boolean> onChange) {
            this.onChange = onChange;
            return this;
        }

        // Note: args and kwargs parameters are not implemented as per CLAUDE.md instructions

        @Override
        public CheckboxComponent build() {
            return new CheckboxComponent(this);
        }
    }

    @Override
    protected String register() {
        final StringWriter writer = new StringWriter();
        registerTemplate.execute(writer, this);
        return writer.toString();
    }

    @Override
    protected String render() {
        final StringWriter writer = new StringWriter();
        renderTemplate.execute(writer, this);
        return writer.toString();
    }

    @Override
    protected TypeReference<Boolean> getTypeReference() {
        return new TypeReference<>() {};
    }

    @Override
    protected void resetIfNeeded() {
        // Checkbox maintains its state - no reset needed unlike button
    }
}