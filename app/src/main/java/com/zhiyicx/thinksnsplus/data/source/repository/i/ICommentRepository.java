package com.zhiyicx.thinksnsplus.data.source.repository.i;

import com.zhiyicx.common.base.BaseJson;
import com.zhiyicx.common.base.BaseJsonV2;
import com.zhiyicx.thinksnsplus.data.beans.PurChasesBean;
import com.zhiyicx.thinksnsplus.data.beans.UserInfoBean;

import rx.Observable;

/**
 * @Describe
 * @Author Jungle68
 * @Date 2017/4/14
 * @Contact master.jungle68@gmail.com
 */

public interface ICommentRepository {

    Observable<BaseJsonV2<Object>> sendCommentV2(String comment_content, long reply_to_user_id, long comment_mark, String path);
    Observable<BaseJson<Object>> sendComment(String comment_content, long reply_to_user_id, long comment_mark, String path);

    Observable<PurChasesBean> checkNote(int note);

    Observable<BaseJsonV2<String>> paykNote(int note,String psd);

    Observable<UserInfoBean> getCurrentLoginUserInfo();

}
