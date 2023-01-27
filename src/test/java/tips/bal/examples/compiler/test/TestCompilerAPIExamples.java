/*
 * MIT License
 *
 * Copyright (c) 2023 Hasitha Aravinda. All rights reserved.
 *
 * This work is licensed under the terms of the MIT license.
 * For a copy, see <https://opensource.org/licenses/MIT>.
 */
package tips.bal.examples.compiler.test;

import org.testng.Assert;
import org.testng.annotations.Test;
import tips.bal.examples.compiler.AnnotationAnalyser;

import java.util.List;

/**
 * Test Cases for Compiler API Example.
 *
 * @since 1.0.0
 */
public class TestCompilerAPIExamples {

    @Test
    public void testAnnotationAnalyzer() {
        AnnotationAnalyser annotationAnalyser = new AnnotationAnalyser();
        final List<String> strings = annotationAnalyser.analyseStudentAnnotations();
        Assert.assertEquals(strings.size(), 7);
        Assert.assertEquals(strings.get(0), "Annotation constraint:Int field: minValueExclusive value: 0 type: 0");
        Assert.assertEquals(strings.get(1), "Annotation constraint:Int field: maxValueExclusive value: 5 type: int");
        Assert.assertEquals(strings.get(2), "Annotation Data field: max value: 5 type: int");
        Assert.assertEquals(strings.get(3), "Annotation Data field: min value: -10 type: int");
        Assert.assertEquals(strings.get(4), "Annotation Data field: pattern value: \"Marks\" type: string");
        Assert.assertEquals(strings.get(5), "Annotation Data field: active value: false type: boolean");
        Assert.assertEquals(strings.get(6), "Annotation Data field: \"low\" value: -9223372036854775808 type: -9223372036854775808");
    }
}
