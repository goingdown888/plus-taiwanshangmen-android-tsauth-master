package com.zhiyicx.thinksnsplus.modules.wallet.integration.recharge

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.widget.Toolbar
import android.text.TextUtils
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView

import com.jakewharton.rxbinding.view.RxView
import com.jakewharton.rxbinding.widget.RxTextView
import com.trycatch.mysnackbar.Prompt
import com.zhiyicx.baseproject.base.TSFragment
import com.zhiyicx.baseproject.config.PayConfig
import com.zhiyicx.baseproject.widget.button.CombinationButton
import com.zhiyicx.baseproject.widget.popwindow.ActionPopupWindow
import com.zhiyicx.common.config.ConstantConfig
import com.zhiyicx.common.utils.DeviceUtils
import com.zhiyicx.common.utils.UIUtils
import com.zhiyicx.common.widget.popwindow.CustomPopupWindow
import com.zhiyicx.thinksnsplus.R
import com.zhiyicx.baseproject.base.SystemConfigBean.IntegrationConfigBean
import com.zhiyicx.thinksnsplus.modules.wallet.integration.detail.recharge_withdrawal.IntegrationRWDetailActivity
import com.zhiyicx.thinksnsplus.modules.wallet.integration.detail.recharge_withdrawal.IntegrationRWDetailContainerFragment
import com.zhiyicx.thinksnsplus.modules.wallet.rule.WalletRuleActivity
import com.zhiyicx.thinksnsplus.modules.wallet.rule.WalletRuleFragment
import com.zhiyicx.thinksnsplus.widget.chooseview.ChooseDataBean
import com.zhiyicx.thinksnsplus.widget.chooseview.SingleChooseView
import com.zhiyicx.tspay.TSPayClient

import java.util.ArrayList
import java.util.Arrays
import java.util.concurrent.TimeUnit

import butterknife.BindView

import com.zhiyicx.common.config.ConstantConfig.JITTER_SPACING_TIME

/**
 * @Describe
 * @Author Jungle68
 * @Date 2017/05/22
 * @Contact master.jungle68@gmail.com
 */
class IntegrationRechargeFragment : TSFragment<IntegrationRechargeContract.Presenter>(), IntegrationRechargeContract.View, SingleChooseView.OnItemChooseChangeListener {

    @BindView(R.id.tv_recharge_ratio)
    lateinit var mTvMineIntegration: TextView
    @BindView(R.id.tv_recharge_rule)
    lateinit var mTvRechargeRule: TextView
    @BindView(R.id.tv_toolbar_center)
    lateinit var mTvToolbarCenter: TextView
    @BindView(R.id.tv_toolbar_left)
    lateinit var mTvToolbarLeft: TextView
    @BindView(R.id.tv_toolbar_right)
    lateinit var mTvToolbarRight: TextView
    @BindView(R.id.toolbar)
    lateinit var mToolbar: Toolbar
    @BindView(R.id.choose_view)
    lateinit var mChooseView: SingleChooseView
    @BindView(R.id.ll_recharge_choose_money_item)
    lateinit var mLlRechargeChooseMoneyItem: LinearLayout
    @BindView(R.id.et_input)
    lateinit var mEtInput: EditText
    @BindView(R.id.bt_recharge_style)
    lateinit var mBtRechargeStyle: CombinationButton
    @BindView(R.id.bt_sure)
    lateinit var mBtSure: TextView

    private var mPayType: String? = null     // type for recharge

    /**
     * 充值提示规则选择弹框
     */
    private var mPayStylePopupWindow: ActionPopupWindow? = null// pay type choose pop
    private var mRechargeInstructionsPopupWindow: ActionPopupWindow? = null// recharge instruction pop


    override var money: Double = 0.toDouble() // money choosed for recharge

    private var mGoldName: String? = null

    override fun useEventBus() = true

    override fun showToolBarDivider() = false

    override fun setUseSatusbar() = true

    override fun setUseStatusView() = false

    override fun showToolbar() = false

    override fun setStatusbarGrey() = false

    override fun getRightViewOfMusicWindow(): View {
        return mTvToolbarRight
    }

    override fun getBodyLayoutId() = R.layout.fragment_integration_recharge

    private val mBaseRatioNum = 1

    override fun initView(rootView: View) {
        setStatusPlaceholderViewBackgroundColor(android.R.color.transparent)
        mIvRefresh = mRootView.findViewById(R.id.iv_refresh)
        mToolbar.setBackgroundResource(android.R.color.transparent)
        (mToolbar.layoutParams as LinearLayout.LayoutParams).setMargins(0, DeviceUtils.getStatuBarHeight(mActivity), 0, 0)
        mTvToolbarCenter.setTextColor(ContextCompat.getColor(mActivity, R.color.white))
        mGoldName = mPresenter.goldName
        mTvToolbarCenter.text = getString(R.string.recharge_integration_foramt, mGoldName)
        mTvToolbarRight.text = getString(R.string.recharge_record)
        mTvToolbarLeft.setCompoundDrawables(UIUtils.getCompoundDrawables(context, R.mipmap.topbar_back_white), null, null, null)

        mLlRechargeChooseMoneyItem.visibility = View.VISIBLE
    }

    override fun initData() {

        mSystemConfigBean = mPresenter.systemConfigBean
        updateData()

    }

    override fun handleLoading(isShow: Boolean) {
        if (isShow) {
            showLeftTopLoading()
        } else {
            hideLeftTopLoading()
        }
    }

    override fun getCurrentActivity(): Activity {
        return mActivity
    }


    private fun updateData() {
        initListener()
        // 元对应的积分比例，服务器返回的是以分为单位的比例
        setDynamicRatio(mBaseRatioNum.toDouble())
        if (!TextUtils.isEmpty(mSystemConfigBean.currency.settings.rechargeoptions)) {
            val datas = ArrayList<ChooseDataBean>()
            val rechargeoptions = mSystemConfigBean.currency.settings.rechargeoptions.split(ConstantConfig.SPLIT_SMBOL.toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            rechargeoptions
                    .filterNot { TextUtils.isEmpty(it) }
                    .forEach {
                        try {
                            val chooseDataBean = ChooseDataBean()
                            chooseDataBean.text = getString(R.string.money_format, PayConfig.realCurrencyFen2Yuan(java.lang.Double.parseDouble(it)))
                            datas.add(chooseDataBean)
                        } catch (e: Exception) {
                        }
                    }
            mChooseView.updateData(datas)
            mChooseView.setOnItemChooseChangeListener(this)
        } else {
            mChooseView.visibility = View.GONE
        }
    }

    /**
     * 动态显示提取金额
     *
     * @param currentIntegration
     */
    private fun setDynamicRatio(currentIntegration: Double) {
        // %1$d元=%2$d%3$s
        val text = "${currentIntegration}元=${currentIntegration * mSystemConfigBean.currency.settings.rechargeratio * (100)}$mGoldName"
        mTvMineIntegration.text = text
    }

    private fun initListener() {
        // 充值协议
        RxView.clicks(mTvRechargeRule)
                .throttleFirst(JITTER_SPACING_TIME.toLong(), TimeUnit.SECONDS)
                .subscribe {
                    val intent = Intent(mActivity, WalletRuleActivity::class.java)
                    val bundle = Bundle()
                    bundle.putString(WalletRuleFragment.BUNDLE_RULE, mSystemConfigBean.currency.recharge.rule)
                    bundle.putString(WalletRuleFragment.BUNDLE_TITLE, resources.getString(R.string.user_reharge_integration_rule))
                    intent.putExtras(bundle)
                    startActivity(intent)
                }
        // 充值记录
        RxView.clicks(mTvToolbarRight)
                .throttleFirst(JITTER_SPACING_TIME.toLong(), TimeUnit.SECONDS)
                .subscribe {
                    val intent = Intent(mActivity, IntegrationRWDetailActivity::class.java)
                    intent.putExtra(IntegrationRWDetailContainerFragment.BUNDLE_DEFAULT_POSITION, IntegrationRWDetailContainerFragment.POSITION_RECHARGE_RECORD)
                    startActivity(intent)
                }
        // 返回
        RxView.clicks(mTvToolbarLeft)
                .throttleFirst(JITTER_SPACING_TIME.toLong(), TimeUnit.SECONDS)
                .subscribe { mActivity.finish() }


        // 选择充值方式
        RxView.clicks(mBtRechargeStyle)
                .throttleFirst(JITTER_SPACING_TIME.toLong(), TimeUnit.SECONDS)   //两秒钟之内只取一个点击事件，防抖操作
                .subscribe {
                    DeviceUtils.hideSoftKeyboard(context, mBtRechargeStyle)
                    initPayStylePop()
                }
        // 确认
        RxView.clicks(mBtSure)
                .throttleFirst(JITTER_SPACING_TIME.toLong(), TimeUnit.SECONDS)   //两秒钟之内只取一个点击事件，防抖操作
                .subscribe {
                    when {
                        PayConfig.realCurrencyYuan2Fen(money) < mSystemConfigBean.currency.settings.rechargemin -> showSnackErrorMessage(getString(R.string.please_more_than_min_recharge_formart, PayConfig.realCurrencyFen2Yuan(
                                mSystemConfigBean.currency.settings.rechargemin.toDouble())))
                        PayConfig.realCurrencyYuan2Fen(money) > mSystemConfigBean.currency.settings.rechargemax -> showSnackErrorMessage(getString(R.string.please_less_min_recharge_formart, PayConfig.realCurrencyFen2Yuan(
                                mSystemConfigBean.currency.settings.rechargemax.toDouble())))
                        else -> {
                            mBtSure.isEnabled = false
                            // 传入的是真实货币分单位
                            mPresenter.getPayStr(mPayType!!, PayConfig.realCurrencyYuan2Fen(money))
                        }
                    }
                }

        RxTextView.textChanges(mEtInput).subscribe({ charSequence ->
            val mRechargeMoneyStr = charSequence.toString()
            if (mRechargeMoneyStr.replace(" ".toRegex(), "").isNotEmpty()) {
                money = java.lang.Double.parseDouble(mRechargeMoneyStr)
                mChooseView.clearChoose()
            } else {
                money = 0.0
            }
            configSureButton()
        }) { throwable ->
            throwable.printStackTrace()
            setCustomMoneyDefault()
            money = 0.0
            configSureButton()
        }

    }


    /**
     * 设置自定义金额数量
     */
    private fun setCustomMoneyDefault() {
        mEtInput.setText("")
    }

    /**
     * 检查确认按钮是否可点击
     */
    private fun configSureButton() {
        mBtSure.isEnabled = money > 0 && !TextUtils.isEmpty(mBtRechargeStyle.rightText)
        setDynamicRatio(money)
    }

    /**
     * 充值方式选择弹框
     */
    private fun initPayStylePop() {
        mSystemConfigBean = mPresenter.systemConfigBean

        val rechargeTypes = ArrayList<String>()
        if (mSystemConfigBean.wallet.recharge.types != null) {
            rechargeTypes.addAll(Arrays.asList(*mSystemConfigBean.wallet.recharge.types))
        }

        if (mPayStylePopupWindow != null) {
            mPayStylePopupWindow!!.show()
            return
        }
        mPayStylePopupWindow = ActionPopupWindow.builder()
                .item2Str(if (rechargeTypes.contains(TSPayClient.CHANNEL_ALIPAY))
                    getString(R.string.choose_pay_style_formart, getString(R.string
                            .alipay))
                else
                    "")
                .item3Str(if (rechargeTypes.contains(TSPayClient.CHANNEL_WXPAY) || rechargeTypes.contains(TSPayClient.CHANNEL_WX))
                    getString(R.string
                            .choose_pay_style_formart, getString(R.string
                            .wxpay))
                else
                    "")
                .item1Str(if (mSystemConfigBean.wallet.isWalletTransform)
                    getString(R.string
                            .choose_pay_style_formart, getString(R.string.wallet_balance))
                else
                    "")
                .item4Str(if (rechargeTypes.size == 0 && (!mSystemConfigBean.wallet
                                .isWalletTransform))
                    getString(R.string.recharge_disallow)
                else
                    "")
                .bottomStr(getString(R.string.cancel))
                .isOutsideTouch(true)
                .isFocus(true)
                .backgroundAlpha(CustomPopupWindow.POPUPWINDOW_ALPHA)
                .with(activity)
                .item2ClickListener {
                    mPayType = TSPayClient.CHANNEL_ALIPAY_V2
                    mBtRechargeStyle.rightText = getString(R.string.choose_recharge_style_formart, getString(R.string.alipay))
                    mPayStylePopupWindow!!.hide()
                    configSureButton()
                }
                .item3ClickListener {
                    mPayType = TSPayClient.CHANNEL_WXPAY_V2
                    mBtRechargeStyle.rightText = getString(R.string.choose_recharge_style_formart, getString(R.string.wxpay))
                    mPayStylePopupWindow!!.hide()
                    configSureButton()
                }
                .item1ClickListener {
                    mPayType = TSPayClient.CHANNEL_BALANCE
                    mBtRechargeStyle.rightText = getString(R.string.choose_recharge_style_formart, getString(R.string.wallet_balance))
                    mPayStylePopupWindow!!.hide()
                    configSureButton()
                }
                .bottomClickListener { mPayStylePopupWindow!!.hide() }
                .build()
        mPayStylePopupWindow!!.show()
    }


    override fun onItemChooseChanged(position: Int, dataBean: ChooseDataBean?) {
        if (position != -1) {
            mEtInput.setText("")
            try {
                if (dataBean != null) {
                    money = java.lang.Double.parseDouble(dataBean.text)
                }
                configSureButton()
            } catch (e: Exception) {
                e.printStackTrace()
            }

        }
    }

    override fun configSureBtn(enable: Boolean) {
        mBtSure.isEnabled = enable
    }

    override fun rechargeSuccess(amount: Double) {
        showSnackSuccessMessage(getString(R.string.integration_recharge_success_tip_format, PayConfig.realCurrencyFen2Yuan(amount), (amount *
                mSystemConfigBean.currency.settings
                        .rechargeratio).toLong(), mGoldName))

    }

    override fun snackViewDismissWhenTimeOut(prompt: Prompt, message: String) {
        if (activity != null && Prompt.SUCCESS == prompt && message.contains(getString(R.string.integration_recharge_success_tip_format_eques))) {
            activity?.finish()
        }
    }

    /**
     * 充值说明选择弹框
     */
    override fun initmRechargeInstructionsPop() {
        if (mRechargeInstructionsPopupWindow != null) {
            mRechargeInstructionsPopupWindow!!.show()
            return
        }
        mRechargeInstructionsPopupWindow = ActionPopupWindow.builder()
                .item1Str(getString(R.string.recharge_instructions))
                .desStr(getString(R.string.recharge_instructions_detail))
                .bottomStr(getString(R.string.cancel))
                .isOutsideTouch(true)
                .isFocus(true)
                .backgroundAlpha(CustomPopupWindow.POPUPWINDOW_ALPHA)
                .with(activity)
                .bottomClickListener { mRechargeInstructionsPopupWindow!!.hide() }
                .build()
        mRechargeInstructionsPopupWindow!!.show()
    }

    override fun useInputMonye() = !mEtInput.text.toString().isEmpty()

    companion object {
        const val BUNDLE_DATA = "data"


        fun newInstance(bundle: Bundle?): IntegrationRechargeFragment {
            val integrationRechargeFragment = IntegrationRechargeFragment()
            integrationRechargeFragment.arguments = bundle
            return integrationRechargeFragment
        }
    }
}
