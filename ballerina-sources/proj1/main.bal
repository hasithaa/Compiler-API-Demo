import ballerina/constraint;
import ballerina/io;

public function main() returns error? {
    Student student = {name: "John Doe", marks: 100};
    io:println(student);
    student = check constraint:validate({name: "John Doe", marks: 100});
    io:println(student);
}

type Student record {|
    string name;
    @constraint:Int {
        minValueExclusive: MIN_VALUE,
        maxValueExclusive: 5
    }
    @Data {
        max: 5,
        min: -10,
        pattern: "Marks",
        active: false,
        "low": int:MIN_VALUE
    }
    int marks;
|};
