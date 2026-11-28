package model.manager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import common.error.ApiException;
import common.error.ErrorCode;
import model.dao.JDBCUtil;
import model.dao.PerformanceDAO;
import model.domain.Performance;
import model.domain.SeatGrade;
import model.domain.VenueRentalProof;
import model.dto.PerformanceRegisterRequest;
import model.dto.PerformanceRegisterRequest.GradeInput;
import model.dto.PerformanceRegisterRequest.RentalInput;
import model.storage.FileStorage;
import model.storage.StoredFile;

@Service
public class PerformanceManager {
    private final PerformanceDAO performanceDAO;
    private final SeatManager seatManager;
    private final FileStorage fileStorage;

    public PerformanceManager(PerformanceDAO performanceDAO, SeatManager seatManager, FileStorage fileStorage) {
        this.performanceDAO = performanceDAO;
        this.seatManager = seatManager;
        this.fileStorage = fileStorage;
    }

    public long registerPerformance(long registrantId, PerformanceRegisterRequest req,
            MultipartFile proof, MultipartFile poster) throws Exception {
        validate(req);
        fileStorage.validateProof(proof);
        fileStorage.validatePoster(poster);

        JDBCUtil jdbcUtil = new JDBCUtil();              // 트랜잭션 소유자 = Manager
        List<StoredFile> savedFiles = new ArrayList<>();

        try {
            // 1. 포스터: 공연 INSERT에 URL이 필요해서 먼저 저장 (선택)
            Performance p = new Performance();
            if (poster != null && !poster.isEmpty()) {
                StoredFile posterFile = fileStorage.savePoster(poster);
                savedFiles.add(posterFile);
                p.setPosterUrl(posterFile.getWebPath());
            }

            // 2. 공연 (심사중 P)
            p.setRegistrantId(registrantId);
            p.setVenueTemplateId(req.getVenueTemplateId());
            p.setPerformanceName(req.getPerformanceName());
            p.setGenre(req.getGenre());
            p.setCastInfo(req.getCastInfo());
            p.setDescription(req.getDescription());
            p.setRunningTime(req.getRunningTime());
            p.setShowAt(req.getShowAt());
            p.setBookingOpenAt(req.getBookingOpenAt());
            p.setCancelDeadlineAt(req.getCancelDeadlineAt());
            p.setMaxTicketsPerMember(req.getMaxTicketsPerMember());
            p.setVenueName(req.getVenueName());
            p.setVenueAddress(req.getVenueAddress());
            long perfId = performanceDAO.create(jdbcUtil, p);

            // 3. 등급: 임시키 → 실제 grade_id
            Map<String, Long> gradeIdByKey = new HashMap<>();
            for (GradeInput g : req.getGrades()) {
                SeatGrade grade = new SeatGrade();
                grade.setPerformanceId(perfId);
                grade.setGradeName(g.getGradeName());
                grade.setPrice(g.getPrice());
                gradeIdByKey.put(g.getGradeKey(), performanceDAO.createGrade(jdbcUtil, grade));
            }

            // 4. (VENUE) + 좌석 배치 + 구역 + 좌석
            seatManager.createLayout(jdbcUtil, perfId, req.getVenueTemplateId(),
                    req.getVenueName(), req.getVenueAddress(), req.getLayout(), gradeIdByKey);

            // 5. 대관 증빙: 롤백이 안 되는 파일 저장은 맨 뒤
            StoredFile proofFile = fileStorage.saveProof(proof);
            savedFiles.add(proofFile);

            VenueRentalProof vp = new VenueRentalProof();
            vp.setPerformanceId(perfId);
            vp.setFilePath(proofFile.getWebPath());
            vp.setOriginalFileName(proofFile.getOriginalName());
            vp.setRentalStartAt(req.getRental().getRentalStartAt());
            vp.setRentalEndAt(req.getRental().getRentalEndAt());
            performanceDAO.createRentalProof(jdbcUtil, vp);

            jdbcUtil.commit();
            return perfId;
        } catch (Exception e) {
            // 순서 중요: Oracle은 commit 없이 close하면 자동 커밋하므로 rollback이 먼저
            try { jdbcUtil.rollback(); } catch (Exception ignore) { }
            for (StoredFile f : savedFiles) fileStorage.delete(f);
            throw e;
        } finally {
            jdbcUtil.close();
        }
    }

    private void validate(PerformanceRegisterRequest r) {
        if (r == null) throw bad("요청이 비어 있습니다.");
        if (isBlank(r.getPerformanceName()) || r.getPerformanceName().length() > 200)
            throw bad("공연명은 1~200자여야 합니다.");
        if (isBlank(r.getGenre()) || isBlank(r.getVenueName()))
            throw bad("장르와 공연장 이름은 필수입니다.");
        if (r.getVenueName().length() > 100) throw bad("공연장 이름은 100자 이하여야 합니다.");
        if (r.getVenueAddress() != null && r.getVenueAddress().length() > 255)
            throw bad("공연장 주소는 255자 이하여야 합니다.");
        if (r.getRunningTime() <= 0 || r.getRunningTime() > 9999)
            throw bad("러닝타임이 올바르지 않습니다.");
        if (r.getMaxTicketsPerMember() < 1 || r.getMaxTicketsPerMember() > 99)
            throw bad("1인 최대 예매 매수는 1~99여야 합니다.");

        // DB CHECK(CK_PERF_DATES)와 같은 규칙
        if (r.getShowAt() == null || r.getBookingOpenAt() == null || r.getCancelDeadlineAt() == null)
            throw bad("공연·예매 오픈·취소 마감 일시는 필수입니다.");
        if (!r.getBookingOpenAt().before(r.getShowAt()))
            throw bad("예매 오픈은 공연 일시보다 앞서야 합니다.");
        if (r.getCancelDeadlineAt().after(r.getShowAt()))
            throw bad("취소 마감은 공연 일시 이전이어야 합니다.");

        // 대관 기간은 공연 일시를 포함해야 함
        RentalInput rental = r.getRental();
        if (rental == null || rental.getRentalStartAt() == null || rental.getRentalEndAt() == null)
            throw bad("대관 기간은 필수입니다.");
        if (rental.getRentalStartAt().after(r.getShowAt()) || rental.getRentalEndAt().before(r.getShowAt()))
            throw bad("대관 기간이 공연 일시를 포함해야 합니다.");

        // 등급
        if (r.getGrades() == null || r.getGrades().isEmpty())
            throw bad("좌석 등급이 하나 이상 필요합니다.");
        Set<String> gradeKeys = new HashSet<>();
        for (GradeInput g : r.getGrades()) {
            if (isBlank(g.getGradeKey()) || !gradeKeys.add(g.getGradeKey()))
                throw bad("등급 키가 비었거나 중복되었습니다.");
            if (isBlank(g.getGradeName()) || g.getGradeName().length() > 20)
                throw bad("등급명은 1~20자여야 합니다.");
            if (g.getPrice() < 0 || g.getPrice() > 99_999_999)
                throw bad("가격이 올바르지 않습니다.");
        }

        // 원본 템플릿(sourceLayoutId)이 있으면 그 공연장(venueTemplateId)도 있어야 한다
        if (r.getLayout() != null && r.getLayout().getSourceLayoutId() != null && r.getVenueTemplateId() == null)
            throw bad("템플릿을 불러왔다면 공연장 템플릿 정보가 필요합니다.");

        seatManager.validateLayout(r.getLayout(), gradeKeys);
    }

    private ApiException bad(String message) {
        return new ApiException(ErrorCode.BAD_REQUEST, message);
    }

    private boolean isBlank(String s) { return s == null || s.isBlank(); }
}