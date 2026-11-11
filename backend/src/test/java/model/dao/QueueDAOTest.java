
package model.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

import org.junit.Before;
import org.junit.Test;

import model.domain.Queue;
import model.dto.QueueStatusDTO;

public class QueueDAOTest {

    private QueueDAO queueDAO;
    private final Long nonExistingId = Long.MAX_VALUE;

    @Before
    public void setUp() {
        queueDAO = new QueueDAO();
    }

    @Test
    public void testSelectActiveQueue() throws Exception {
        Queue result = queueDAO.selectActiveQueue(
                nonExistingId, nonExistingId);

        assertNull(result);
    }

    @Test
    public void testSelectQueueStatus() throws Exception {
        QueueStatusDTO result =
                queueDAO.selectQueueStatus(nonExistingId);

        assertNull(result);
    }

    @Test
    public void testUpdateQueueStatus() throws Exception {
        int result = queueDAO.updateQueueStatus(
                nonExistingId, "W");

        assertEquals(0, result);
    }

    @Test
    public void testIssueAuthCode() throws Exception {
        int result = queueDAO.issueAuthCode(
                nonExistingId, "123456");

        assertEquals(0, result);
    }

    @Test
    public void testVerifyAuthCode() throws Exception {
        int result = queueDAO.verifyAuthCode(
                nonExistingId, "123456");

        assertEquals(0, result);
    }

    @Test
    public void testIsVerified() throws Exception {
        boolean result = queueDAO.isVerified(nonExistingId);

        assertFalse(result);
    }
}
