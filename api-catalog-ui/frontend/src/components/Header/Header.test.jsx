/* eslint-disable no-undef */
import * as React from 'react';
// tslint:disable-next-line:no-implicit-dependencies
import * as enzyme from 'enzyme';
import Header from './Header';

describe('>>> Header component tests', () => {
    it('should display a Link', () => {
        const sample = enzyme.shallow(<Header />);
        expect(sample.find('Link')).toBeDefined();
    });

    it('should have link href to itself', () => {
        const sample = enzyme.shallow(<Header />);
        expect(sample.find('Link').props().href).toEqual('/#/dashboard');
    });

    it('should handle a Logout button click', () => {
        const logout = jest.fn();
        const wrapper = enzyme.shallow(<Header logout={logout} />);
        wrapper.find('Button').simulate('click');
        expect(logout).toHaveBeenCalled();
    });

    it('should handle a Logout button click', () => {
        const logout = jest.fn();
        const wrapper = enzyme.shallow(<Header logout={logout} />);
        const instance = wrapper.instance();
        instance.handleLogout();
        expect(logout).toHaveBeenCalled();
    });
});
