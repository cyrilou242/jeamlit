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
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public class RadioComponent extends JtComponent<Object> {
    // Protected fields for mustache template access
    protected final String label;
    protected final List<Object> options;
    protected final @Nullable Integer index;
    protected final @Nullable Function<Object, String> formatFunc;
    protected final @Nullable String help;
    protected final boolean disabled;
    protected final boolean horizontal;
    protected final @Nullable List<String> captions;
    protected final LabelVisibility labelVisibility;
    protected final String width;

    // Computed fields for template rendering
    protected final List<RadioOption> radioOptions;
    protected final String optionsJson;
    protected final String captionsJson;

    private static final Mustache registerTemplate;
    private static final Mustache renderTemplate;

    static {
        final MustacheFactory mf = new DefaultMustacheFactory();
        registerTemplate = mf.compile("components/input/RadioComponent.register.html.mustache");
        renderTemplate = mf.compile("components/input/RadioComponent.render.html.mustache");
    }

    private RadioComponent(Builder builder) {
        super(builder.generateKeyForInteractive(), 
              computeInitialValue(builder.options, builder.index), 
              builder.onChange);

        this.label = builder.label;
        this.options = new ArrayList<>(builder.options);
        this.index = builder.index;
        this.formatFunc = builder.formatFunc;
        this.help = builder.help;
        this.disabled = builder.disabled;
        this.horizontal = builder.horizontal;
        this.captions = builder.captions != null ? new ArrayList<>(builder.captions) : null;
        this.labelVisibility = builder.labelVisibility;
        this.width = builder.width;

        // Compute derived fields for template rendering
        this.radioOptions = buildRadioOptions();
        this.optionsJson = toJson(this.options);
        this.captionsJson = this.captions != null ? toJson(this.captions) : "null";
    }

    /**
     * Helper class to represent radio options for template rendering
     */
    public static class RadioOption {
        public final int index;
        public final Object value;
        public final String displayText;
        public final boolean selected;
        public final @Nullable String caption;

        public RadioOption(int index, Object value, String displayText, boolean selected, @Nullable String caption) {
            this.index = index;
            this.value = value;
            this.displayText = displayText;
            this.selected = selected;
            this.caption = caption;
        }
    }

    private List<RadioOption> buildRadioOptions() {
        final List<RadioOption> radioOptions = new ArrayList<>();
        final Object selectedValue = this.currentValue;

        for (int i = 0; i < options.size(); i++) {
            final Object option = options.get(i);
            final String displayText = formatFunc != null ? formatFunc.apply(option) : String.valueOf(option);
            final boolean selected = (selectedValue != null && selectedValue.equals(option));
            final String caption = (captions != null && i < captions.size()) ? captions.get(i) : null;
            
            radioOptions.add(new RadioOption(i, option, displayText, selected, caption));
        }

        return radioOptions;
    }

    private static Object computeInitialValue(List<Object> options, @Nullable Integer index) {
        if (options.isEmpty()) {
            return null;
        }
        if (index == null) {
            return null; // No initial selection
        }
        if (index < 0 || index >= options.size()) {
            throw new IllegalArgumentException("index out of bounds: " + index + ", options size: " + options.size());
        }
        return options.get(index);
    }

    public static class Builder extends JtComponentBuilder<Object, RadioComponent, Builder> {
        private final String label;
        private final List<Object> options;
        private @Nullable Integer index = 0; // Default to first option as per spec
        private @Nullable Function<Object, String> formatFunc = null;
        private @Nullable String help = null;
        private @Nullable Consumer<Object> onChange = null;
        // Note: args/kwargs equivalent (varargs and Map parameters) not implemented
        private boolean disabled = false;
        private boolean horizontal = false;
        private @Nullable List<String> captions = null;
        private LabelVisibility labelVisibility = LabelVisibility.VISIBLE;
        private String width = "content";

        public Builder(@Nonnull String label, @Nonnull List<Object> options) {
            if (label == null || label.trim().isEmpty()) {
                throw new IllegalArgumentException("Radio label cannot be null or empty");
            }
            if (options == null || options.isEmpty()) {
                throw new IllegalArgumentException("Radio options cannot be null or empty");
            }
            this.label = label;
            this.options = new ArrayList<>(options);
        }

        public Builder index(@Nullable Integer index) {
            this.index = index;
            return this;
        }

        public Builder formatFunc(@Nullable Function<Object, String> formatFunc) {
            this.formatFunc = formatFunc;
            return this;
        }

        public Builder help(@Nullable String help) {
            this.help = help;
            return this;
        }

        public Builder onChange(@Nullable Consumer<Object> onChange) {
            this.onChange = onChange;
            return this;
        }

        public Builder disabled(boolean disabled) {
            this.disabled = disabled;
            return this;
        }

        public Builder horizontal(boolean horizontal) {
            this.horizontal = horizontal;
            return this;
        }

        public Builder captions(@Nullable List<String> captions) {
            this.captions = captions;
            return this;
        }

        public Builder labelVisibility(@Nonnull LabelVisibility labelVisibility) {
            this.labelVisibility = labelVisibility;
            return this;
        }

        public Builder width(@Nonnull String width) {
            if (width == null) {
                throw new IllegalArgumentException("width cannot be null");
            }
            
            // Validate width parameter
            if (!width.equals("content") && !width.equals("stretch") && !width.matches("\\d+")) {
                throw new IllegalArgumentException("width must be 'content', 'stretch', or a pixel value (integer). Got: " + width);
            }
            this.width = width;
            return this;
        }

        @Override
        public RadioComponent build() {
            // Validate index bounds if provided
            if (index != null && (index < 0 || index >= options.size())) {
                throw new IllegalArgumentException("index out of bounds: " + index + ", options size: " + options.size());
            }

            // Validate captions length if provided
            if (captions != null && captions.size() != options.size()) {
                throw new IllegalArgumentException("captions list size (" + captions.size() + 
                                                 ") must match options list size (" + options.size() + ")");
            }

            return new RadioComponent(this);
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
    protected TypeReference<Object> getTypeReference() {
        return new TypeReference<>() {};
    }

    @Override
    protected Object castAndValidate(Object rawValue) {
        // For radio components, the rawValue should be the actual option value
        // We need to validate it's one of our valid options
        if (rawValue == null) {
            return null;
        }

        // Find the option that matches the raw value
        for (Object option : options) {
            if (option.equals(rawValue)) {
                return option;
            }
        }

        // If not found, try to match by string representation
        final String rawString = String.valueOf(rawValue);
        for (Object option : options) {
            if (String.valueOf(option).equals(rawString)) {
                return option;
            }
        }

        throw new IllegalArgumentException("Invalid radio selection: " + rawValue + 
                                         ". Must be one of: " + options);
    }

    @Override
    protected void resetIfNeeded() {
        // Radio keeps its value - no reset needed
    }
}