//DEPS com.google.guava:guava:32.1.3-jre

import tech.catheu.jeamlit.core.Jt;
import com.google.common.collect.Lists;

public class TestJBangIntegration {
    public static void main(String[] args) {
        Jt.title("✅ Testing JBang 0.127.18 Integration - UPGRADED!").use();
        Jt.text("This test verifies that dependency resolution works with the upgraded JBang version.").use();
        
        // Use Guava to test dependency resolution
        var list = Lists.newArrayList("JBang", "0.127.18", "works!");
        Jt.text("Test result: " + String.join(" → ", list)).use();
        
        if (Jt.button("Test Dependency Loading").use()) {
            Jt.text("✅ Guava Lists class loaded successfully! Here's one: ").use();
            Jt.text("📦 Dependency resolution is working!").use();
        }
    }
}