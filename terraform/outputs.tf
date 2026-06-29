output "ec2_instance_id" {
  value       = aws_instance.mes_backend.id
  description = "EC2 instance ID used for start/stop commands."
}

output "ec2_public_ip" {
  value       = aws_instance.mes_backend.public_ip
  description = "Current public IP of the MES backend EC2 instance."
}

output "ssh_command" {
  value       = "ssh -i <path-to-your-pem> ubuntu@${aws_instance.mes_backend.public_ip}"
  description = "SSH command template for the MES backend EC2 instance."
}
