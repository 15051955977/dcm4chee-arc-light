import {Component, ContentChild, ContentChildren, EventEmitter, Input, OnInit, Output, QueryList} from '@angular/core';
import {OptionComponent} from "../dropdown/option.component";
import {SelectDropdown} from "../../interfaces";
import {animate, state, style, transition, trigger} from "@angular/animations";
import {element} from "protractor";

@Component({
    selector: 'dcm-drop-down',
    templateUrl: './dcm-drop-down.component.html',
    styleUrls: ['./dcm-drop-down.component.scss'],
    animations:[
        trigger("showHide",[
            state("show",style({
                padding:"*",
                height:'*',
                opacity:1
            })),
            state("hide",style({
                padding:"0",
                opacity:0,
                height:'0px',
                margin:"0"
            })),
            transition("show => hide",[
                animate('0.1s')
            ]),
            transition("hide => show",[
                animate('0.2s cubic-bezier(.52,-0.01,.15,1)')
            ])
        ])
    ],
})
export class DcmDropDownComponent implements OnInit {
    selectedValue:any;
    selectedDropdown:SelectDropdown;
    isAllCheck:boolean = false;
    multiSelectValue = [];
    @Input() placeholder:string;
    @Input() multiSelectMode:boolean = false;
    @Input() showSearchField:boolean = false;
    @Input() mixedMode:boolean = false;
    @Input() maxSelectedValueShown = 2;
    @Input() options:SelectDropdown[];
    @Input() optionsTree:{label:string, options:SelectDropdown[]}[];
    @Input() showStar:boolean = false;
    @Input('model')
    set model(value){
        if(!(this.selectedDropdown && this.selectedDropdown.value === value) && !this.multiSelectMode){
            this.selectedValue = value;
            this.selectedDropdown  = this.getSelectDropdownFromValue(value);
            // this.setSelectedElement();
        }else{
            this.multiSelectValue = value || [];
            this.setSelectedElement();
        }
    }
    @Output() modelChange =  new EventEmitter();

    showDropdown = false;
    constructor() { }

    ngOnInit() {
    }
    getSelectDropdownFromValue(value):SelectDropdown{
        // let endDropdown:any =  new SelectDropdown(value,'');
        if(value && this.options){
            this.options.forEach(element=>{
                if(element.value === value){
                    return element;
                }
            });
        }
        return undefined;
    }
    allChecked(e){

        if(!this.isAllCheck){
            this.multiSelectValue = [];
        }
        this.options.forEach(element=>{
            element.selected = this.isAllCheck;
            if(this.isAllCheck){
                this.multiSelectValue.push(element.value);
            }
        })
        // this.changeDetectorRef.detectChanges();
    }
    setSelectedElement(){
        if(this.multiSelectMode){
            if(this.options && this.multiSelectValue){
                let count = 0;
                this.options.forEach(element=>{
                    // console.log("uniqueId3",element.uniqueId);
                    if(this.multiSelectValue.indexOf(element.value) > -1){
                        element.selected = true;
                        count++;
                    }else{
                        element.selected = false;
                    }
                });
                if(count === this.options.length-1){ //TODO make clear optional
                    this.isAllCheck = true;
                }
            }
        }else{
            if(this.options && this.selectedValue){
                this.options.forEach(element=>{
                    if(element.value === this.selectedValue || element.value === this.selectedValue){
                        element.selected = true;
                    }else{
                        element.selected = false;
                    }
                });
            }
        }
        // this.changeDetectorRef.detectChanges();
    }
    select(element){
        this.showDropdown = false;
        if(this.multiSelectMode){
            let index = this.multiSelectValue.indexOf(element.value);
            if(index> -1){
                this.multiSelectValue.splice(index, 1);
            }else{
                this.multiSelectValue.push(element.value);
            }
        }else{
            if(!this.mixedMode){
                this.options.forEach(option =>{ option.selected = false;});
            }
            if(element === ""){
                this.selectedValue = "";
                this.selectedDropdown = undefined;
            }else{
                element.selected = true;
                this.selectedDropdown = element;
                this.selectedValue = element.value;
            }
        }
    }
    toggleDropdown(){
        this.showDropdown = !this.showDropdown;
    }
}
