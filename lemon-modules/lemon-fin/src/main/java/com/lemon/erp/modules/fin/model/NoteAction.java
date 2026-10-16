package com.lemon.erp.modules.fin.model;

/**
 * 票据操作，与字典 fin_note_action 对应
 */
public enum NoteAction {
    /** 收票（生成收款单） */
    RECEIVE,
    /** 背书转让（生成付款单） */
    ENDORSE,
    DISCOUNT,
    COLLECT,
    DISHONOR,
    /** 开具应付票据（生成付款单） */
    ISSUE,
    PAY;

    /** 通过收付款单完成的操作（其余直接生成凭证） */
    public boolean viaSettlement() {
        return this == RECEIVE || this == ENDORSE || this == ISSUE;
    }
}
