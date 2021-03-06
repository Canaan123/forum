package matsk.mszdqabbs.Service.Impl;

import matsk.mszdqabbs.DAO.FollowDAO;
import matsk.mszdqabbs.DAO.InviteDAO;
import matsk.mszdqabbs.DAO.QuestionDAO;
import matsk.mszdqabbs.DAO.UserDAO;
import matsk.mszdqabbs.Pojo.Invite;
import matsk.mszdqabbs.Pojo.User;
import matsk.mszdqabbs.Service.InviteService;
import matsk.mszdqabbs.Service.UserService;
import matsk.mszdqabbs.Utils.JacksonUtils;
import matsk.mszdqabbs.Utils.TokenUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

@Service
public class InviteServiceImpl implements InviteService {
    @Autowired
    private InviteDAO inviteDAO;
    @Autowired
    private FollowDAO followDAO;
    @Autowired
    private UserDAO userDAO;
    @Autowired
    private UserService userService;
    @Autowired
    private QuestionDAO questionDAO;

    @Override
    public String getHowManyNotReadInvitationOf(HttpServletRequest request) {
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("success","false");
        Integer be_invited = TokenUtils.getUid(request);
        if(be_invited != null) {
            resultMap.put("howManyNotReadInvitation", inviteDAO.getHowManyNotReadInvitationOf(be_invited));
            resultMap.put("success","true");
        }
        return JacksonUtils.mapToJson(resultMap);
    }

    @Override
    @Transactional
    public String getFollowsAndInivitationState(Integer questionId, HttpServletRequest request) {
        Integer uid = TokenUtils.getUid(request);
        if(uid != null) {
            List<Map<String, Object>> resultList = new ArrayList<>();
            List<User> to_follow = followDAO.getFollowsOf(uid);
            List<Invite> invitationState = inviteDAO.getFollowsInvitationStateOf(questionId, uid);
            to_follow.forEach(to_follow_each -> {
                Map<String, Object> eachFollowMap = new HashMap<>();
                eachFollowMap.put("followId", to_follow_each.getId());
                eachFollowMap.put("headPhotoUrl", to_follow_each.getHead_photo_url());
                eachFollowMap.put("nickname", to_follow_each.getNickname());
                eachFollowMap.put("alreadyInvited","false");
                invitationState.forEach(state -> {
                    //?????????????????????????????????????????????????????????????????????????????????????????????
                    if(state.getBe_invited().equals(to_follow_each.getId()) && state.getInviter().equals(uid)) {
                        eachFollowMap.put("alreadyInvited","true");
                    }
                });
                resultList.add(eachFollowMap);
            });
            try {
                return JacksonUtils.obj2json(resultList);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }
        return null;
    }

    @Override
    @Transactional
    public String invite(Integer questionId, Integer be_invited, HttpServletRequest request) {
        Map<String, String> resultMap = new HashMap<>();
        resultMap.put("success","false");
        Integer uid = TokenUtils.getUid(request);
        if(uid != null && !be_invited.equals(uid)) {//???????????????????????????
            Invite newInvite = new Invite(0, uid, be_invited, questionId, 0, null);
            if(inviteDAO.isAlreadyInvited(newInvite) == 0) { //??????????????????????????????????????????????????????????????????????????????
                if (inviteDAO.invite(newInvite) == 1) {
                    resultMap.put("success", "true");
                }
            }
        }
        return JacksonUtils.mapToJson(resultMap);
    }

    @Override
    @Transactional
    public String getSearchUserAndInvitationState(Integer questionId, String searchStr, HttpServletRequest request) {
        Integer uid = TokenUtils.getUid(request);
        if(uid != null) {
            List<Map<String, Object>> resultList = new ArrayList<>();
            List<User> searchUsers = userDAO.findUsersLikely(searchStr, uid);
            List<Invite> invitationState = inviteDAO.getSearchUsersInvitationStateOf(questionId, uid, searchStr);
            searchUsers.forEach(searchUser -> {
                Map<String, Object> eachSearchUserMap = new HashMap<>();
                eachSearchUserMap.put("userId", searchUser.getId());
                eachSearchUserMap.put("headPhotoUrl", searchUser.getHead_photo_url());
                eachSearchUserMap.put("nickname", searchUser.getNickname());
                eachSearchUserMap.put("alreadyInvited","false");
                invitationState.forEach(state -> {
                    //????????????????????????????????????????????????????????????????????????????????????????????????
                    if(state.getBe_invited().equals(searchUser.getId()) && state.getInviter().equals(uid)) {
                        eachSearchUserMap.put("alreadyInvited","true");
                    }
                });
                resultList.add(eachSearchUserMap);
            });
            try {
                return JacksonUtils.obj2json(resultList);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }
        return null;
    }

    @Override
    @Transactional
    public String getInvitationsOf(HttpServletRequest request) {
        Integer be_invited = TokenUtils.getUid(request);
        if(be_invited != null) {
            List<Invite> myInvitation = inviteDAO.getMyInvitation(be_invited);
            if(myInvitation != null && myInvitation.size() > 0) {
                List<Map<String, Object>> resultList = new ArrayList<>();
                //??????SET?????????????????????????????????????????????????????????????????????????????????
                Set<Integer> myInvitationInviterIds = new HashSet<>();
                Set<Integer> myInvitationQuestionIds = new HashSet<>();
                myInvitation.forEach(invitation -> {
                    myInvitationInviterIds.add(invitation.getInviter());
                    myInvitationQuestionIds.add(invitation.getWhich_question());
                });
                //?????????????????????????????????ID?????????????????????????????????UserService???getUserInfoOfComment??????
                Map<Integer, Map<String, Object>> inviterInfos = new HashMap<>();
                myInvitationInviterIds.forEach(inviter -> {
                    try {
                        inviterInfos.put(inviter, userService.getUserInfoOfComment(inviter));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
                //??????????????????????????????
                Map<Integer, String> invitationQuestionTitles = new HashMap<>();
                myInvitationQuestionIds.forEach(question -> {
                    invitationQuestionTitles.put(question, questionDAO.getQuestionTitleById(question));
                });
                //???????????????
                myInvitation.forEach(invitation -> {
                    Map<String, Object> eachInvitation = new HashMap<>();
                    eachInvitation.put("inviteId", invitation.getId());
                    eachInvitation.put("inviter", inviterInfos.get(invitation.getInviter()));
                    eachInvitation.put("questionId",invitation.getWhich_question());
                    eachInvitation.put("questionTitle", invitationQuestionTitles.get(invitation.getWhich_question()));
                    eachInvitation.put("inviteTime", invitation.getInvite_time());
                    eachInvitation.put("isRead",invitation.getIs_read());
                    resultList.add(eachInvitation);
                });
                //????????????????????????
                resultList.sort((r1,r2) -> {
                    Integer r1IsRead = Integer.parseInt(r1.get("isRead") + "");
                    Integer r2IsRead = Integer.parseInt(r2.get("isRead") + "");
                    return r1IsRead.compareTo(r2IsRead);
                });

                try {
                    return JacksonUtils.obj2json(resultList);
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
            }
        }
        return null;
    }

    @Override
    @Transactional
    public String readInvites(List<Integer> readInviteIds, HttpServletRequest request) {
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("success","false");
        if(TokenUtils.isLogin(request)) {
            readInviteIds.forEach(inviteId -> {
                if(inviteDAO.readInvite(inviteId) != 1) {
                    throw new RuntimeException("????????????????????????????????????");
                }
            });
            resultMap.put("success","true");
        }
        return JacksonUtils.mapToJson(resultMap);
    }
}
