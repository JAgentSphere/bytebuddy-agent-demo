package com.burpsuite;

import java.lang.instrument.Instrumentation;

public class Main {
    public static void premain(String agentArgs, Instrumentation inst) {
        BurpTransformer burpTransformer = new BurpTransformer();
        BountyTransformer bountyTransformer = new BountyTransformer();
        inst.addTransformer(burpTransformer);
        inst.addTransformer(bountyTransformer);
    }
}
