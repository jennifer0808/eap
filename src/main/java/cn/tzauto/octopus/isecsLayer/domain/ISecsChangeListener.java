/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.tzauto.octopus.isecsLayer.domain;

/**
 *
 * @author gavin
 */
public interface ISecsChangeListener {
    void handleChange(ISecsPara oldPara,ISecsPara newPara);
}
