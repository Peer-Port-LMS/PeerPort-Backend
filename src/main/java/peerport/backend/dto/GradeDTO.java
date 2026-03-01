package peerport.backend.dto;

import java.util.Date;
import java.util.Optional;

public class GradeDTO {
    public String gradeId;
    public Optional<Integer> gradeObtained;
    public int maxGrade;

    public Date dateGraded;
    public Date dateUpdated;
}
