package com.example.assessment.config;

import com.example.assessment.dto.AssessmentRequest;
import com.example.assessment.dto.OptionRequest;
import com.example.assessment.dto.QuestionRequest;
import com.example.assessment.entity.AssessmentStatus;
import com.example.assessment.repository.AssessmentRepository;
import com.example.assessment.service.AssessmentService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class DemoAssessmentInitializer {

    private static final String DEMO_TITLE = "Java Basics Demo";

    @Bean
    @ConditionalOnProperty(name = "app.demo.seed-enabled", havingValue = "true", matchIfMissing = true)
    CommandLineRunner seedDemoAssessment(AssessmentRepository assessments, AssessmentService service) {
        return args -> {
            service.ensureDemo(new AssessmentRequest(DEMO_TITLE,
                    "A beginner-friendly introduction to Java fundamentals.",
                    AssessmentStatus.PUBLISHED, questions()));
        };
    }

    private List<QuestionRequest> questions() {
        return List.of(
                question("Which keyword declares a class in Java?", "class", "define", "struct", "type"),
                question("Which method is the entry point of a Java application?", "main", "start", "run", "begin"),
                question("Which type stores true or false values?", "boolean", "binary", "truth", "bit"),
                question("Which collection does not allow duplicate elements?", "Set", "List", "Array", "Queue"),
                question("Which keyword creates an object?", "new", "make", "create", "object"),
                question("What is the default value of an int field?", "0", "1", "null", "-1"),
                question("Which access modifier makes a member available everywhere?", "public", "private", "protected", "internal"),
                question("Which type is commonly used for text?", "String", "Text", "CharArray", "Word"),
                question("Which keyword prevents a method from being overridden?", "final", "static", "fixed", "constant"),
                question("Which loop is guaranteed to execute its body at least once?", "do-while", "for", "while", "foreach"));
    }

    private QuestionRequest question(String text, String correct, String second, String third, String fourth) {
        List<OptionRequest> options = new ArrayList<>();
        options.add(new OptionRequest(correct, true));
        options.add(new OptionRequest(second, false));
        options.add(new OptionRequest(third, false));
        options.add(new OptionRequest(fourth, false));
        return new QuestionRequest(text, 1, options);
    }
}
