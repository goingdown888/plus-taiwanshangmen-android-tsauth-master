package com.zhiyicx.thinksnsplus.modules.personal_center.adapter;

import android.content.Context;

import com.zhiyicx.thinksnsplus.data.beans.DynamicBean;
import com.zhiyicx.thinksnsplus.data.beans.DynamicDetailBeanV2;
import com.zhiyicx.thinksnsplus.data.beans.Letter;
import com.zhy.adapter.recyclerview.base.ViewHolder;

import static com.zhiyicx.thinksnsplus.data.beans.DynamicListAdvert.DEFAULT_ADVERT_FROM_TAG;

/**
 * @author LiuChao
 * @describe
 * @date 2017/3/10
 * @contact email:450127106@qq.com
 */

public class PersonalCenterDynamicListForZeroImage extends PersonalCenterDynamicListBaseItem {
    public PersonalCenterDynamicListForZeroImage(Context context) {
        super(context);
    }

    @Override
    public int getItemViewLayoutId() {
        return super.getItemViewLayoutId();
    }

    @Override
    public boolean isForViewType(DynamicDetailBeanV2 item, int position) {
        Letter letter = item.getMLetter();
        return item.getFeed_mark() != null && item.getFeed_from() != DEFAULT_ADVERT_FROM_TAG && (item.getImages() == null || item.getImages()
                .isEmpty()) && item.getVideo() == null && letter == null;
    }


    @Override
    public void convert(ViewHolder holder, final DynamicDetailBeanV2 dynamicBean, DynamicDetailBeanV2 lastT, int position, int itemCounts) {
        super.convert(holder, dynamicBean, lastT, position,itemCounts);
    }
}
