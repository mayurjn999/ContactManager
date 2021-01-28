function deleteContact(cid)
{
    swal({
  title: "Are you sure?",
  text: "Once deleted, you will not be able to recover this Contact!!",
  icon: "warning",
  buttons: true,
  dangerMode: true,
})
.then((willDelete) => {
  if (willDelete) {
    window.location="/user/delete/"+cid;
  } else {
    swal("Your Contact is safe!!");
  }
});	
    		
 }
  